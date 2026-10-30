package com.example.solution.fde;

import static com.example.solution.fde.EnterpriseRecords.*;
import com.example.solution.security.AccountDirectory;
import com.example.solution.security.SecurityUtils;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class EnterpriseAdministration {
    final EnterpriseAccess access;
    final EnterpriseStore store;
    final ObjectMapper mapper;
    final AccountDirectory users;
    public EnterpriseAdministration(EnterpriseAccess access, EnterpriseStore store, ObjectMapper mapper, AccountDirectory users) {
        this.access = access; this.store = store; this.mapper = mapper; this.users = users;
    }
    static ResponseStatusException bad(String reason) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason); }
    static ResponseStatusException conflict(String reason) { return new ResponseStatusException(HttpStatus.CONFLICT, reason); }
    static void identifier(String value) { if (value == null || !value.matches("[A-Za-z0-9_-]{1,64}")) throw bad("Invalid organization identifier"); }
    private Member admin() {
        access.lockForWrite(); var member = access.member(); if (!member.has("ORG_ADMIN")) throw EnterpriseAccess.denied(); return member;
    }
    void audit(String tenant, String action, String before, Object after) {
        var event = new ConfigAudit(); event.tenant = tenant; event.actor = access.actor(); event.action = action;
        event.beforeData = before; event.afterData = mapper.writeValueAsString(after); event.occurredAt = access.clock.instant(); store.save(event);
    }
    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        var member = access.member(); boolean admin = member.has("ORG_ADMIN");
        var projects = store.projects(member.tenant).stream().filter(p -> admin || member.projectSet().contains(p.code)).map(p -> {
            var row = new LinkedHashMap<String, Object>(); row.put("code", p.code); row.put("department", p.department);
            if (admin || member.has("FINANCE") || member.has("MANAGER") && member.department.equals(p.department)) { row.put("budgetLimit", p.budgetLimit); row.put("spent", p.spent); }
            return row;
        }).toList();
        var delegations = store.delegations(member.tenant).stream().filter(d -> admin || d.grantor.equals(member.login) || d.delegate.equals(member.login)).toList();
        return Map.of("member", member, "projects", projects, "rules", store.rules(member.tenant), "delegations", delegations,
            "members", admin ? store.members(member.tenant) : List.of(), "audit", admin ? store.audits(member.tenant) : List.of());
    }
    public void bootstrap(String tenant, String department) {
        if (!SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN") || store.member(access.actor()).isPresent()) throw EnterpriseAccess.denied();
        identifier(tenant); identifier(department);
        var company = new Tenant(); company.id = tenant; store.save(company);
        var member = new Member(); member.login = access.actor(); member.tenant = tenant; member.department = department;
        member.roles = "ORG_ADMIN"; member.projects = ""; store.save(member);
        var rule = new Rule(); rule.tenant = tenant; rule.key = "V1"; rule.effectiveAt = Instant.EPOCH; store.save(rule);
        audit(tenant, "bootstrap", null, member);
    }
    public Project project(String code, String department, long limit) {
        var admin = admin(); identifier(code); identifier(department);
        if (limit < 0 || limit > 1000000000000L) throw bad("Invalid project budget");
        if (store.project(admin.tenant, code).isPresent()) throw conflict("Project already exists; its balance is not overwritten");
        var project = new Project(); project.tenant = admin.tenant; project.code = code; project.department = department; project.budgetLimit = limit;
        store.save(project); audit(admin.tenant, "project-create", null, project); return project;
    }
    public Member member(String login, String department, Set<String> roles, Set<String> projects, boolean active, Long expectedVersion) {
        var admin = admin(); identifier(department);
        if (login == null || !login.matches("[a-z0-9_-]{1,50}") || roles == null || roles.isEmpty() || projects == null || projects.size() > 50 ||
            !Set.of("STAFF", "MANAGER", "FINANCE", "ORG_ADMIN").containsAll(roles)) throw bad("Invalid membership");
        for (var code : projects) if (store.project(admin.tenant, code).isEmpty()) throw bad("Unknown company project");
        var member = store.member(login).orElse(null); String before = member == null ? null : mapper.writeValueAsString(member);
        if (member != null && !member.tenant.equals(admin.tenant)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (member != null && !Objects.equals(member.version, expectedVersion)) throw conflict("Membership changed; reload");
        if (member == null && (expectedVersion != null || users.findOneByLogin(login).isEmpty())) throw bad("Existing account required for a new membership");
        if (member == null) { member = new Member(); member.login = login; member.tenant = admin.tenant; }
        member.department = department; member.roles = values(roles); member.projects = values(projects); member.active = active;
        if (before == null) store.save(member); else store.flush();
        audit(admin.tenant, "membership-change", before, member); return member;
    }
    public Rule rule(String key, Instant effectiveAt, Long financeAbove) {
        var admin = admin(); identifier(key); var now = access.clock.instant();
        if (financeAbove != null && (financeAbove < 0 || financeAbove > 100000000)) throw bad("Invalid second approval threshold");
        var start = effectiveAt == null ? now : effectiveAt;
        if (start.isBefore(now)) throw bad("New policy cannot be retroactive");
        if (store.rule(admin.tenant, key).isPresent() || store.rules(admin.tenant).stream().anyMatch(r -> r.effectiveAt.equals(start))) throw conflict("Policy versions and effective instants are immutable and unique");
        var rule = new Rule(); rule.tenant = admin.tenant; rule.key = key; rule.effectiveAt = start; rule.financeAbove = financeAbove;
        store.save(rule); audit(admin.tenant, "policy-publish", null, rule); return rule;
    }
    public Delegation delegate(String to, String code, Instant from, Instant until) {
        access.lockForWrite(); var grantor = access.member(); var project = access.project(grantor, code);
        var receiver = store.member(to).filter(m -> m.active && m.tenant.equals(grantor.tenant) && m.projectSet().contains(code)).orElseThrow(EnterpriseAccess::denied);
        if (!grantor.has("MANAGER") || !grantor.department.equals(project.department) || receiver.login.equals(grantor.login)) throw EnterpriseAccess.denied();
        if (from == null || until == null || !until.isAfter(from) || !until.isAfter(access.clock.instant()) || java.time.Duration.between(from, until).compareTo(java.time.Duration.ofHours(24)) > 0) throw bad("Delegation must be a positive interval of at most 24 hours");
        var d = new Delegation(); d.tenant = grantor.tenant; d.grantor = grantor.login; d.delegate = receiver.login; d.project = code; d.validFrom = from; d.validUntil = until;
        store.save(d); audit(grantor.tenant, "delegation-create", null, d); return d;
    }
    public void revoke(long id) {
        access.lockForWrite(); var actor = access.member();
        var d = store.delegations(actor.tenant).stream().filter(item -> item.id == id).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!d.grantor.equals(actor.login) && !actor.has("ORG_ADMIN")) throw EnterpriseAccess.denied();
        if (d.revokedAt == null) { var before = mapper.writeValueAsString(d); d.revokedAt = access.clock.instant(); store.flush(); audit(actor.tenant, "delegation-revoke", before, d); }
    }
}
