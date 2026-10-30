package com.example.solution.fde;

import static com.example.solution.fde.EnterpriseRecords.*;
import com.example.solution.security.SecurityUtils;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Component
public class EnterpriseAccess implements FdePolicy {
    final EnterpriseStore store;
    final Clock clock;
    final ObjectMapper mapper;
    public EnterpriseAccess(EnterpriseStore store, Clock clock, ObjectMapper mapper) { this.store = store; this.clock = clock; this.mapper = mapper; }
    public String id() { return "enterprise"; }
    public static ResponseStatusException denied() { return new ResponseStatusException(HttpStatus.FORBIDDEN, "Current organization policy denies access"); }
    public String actor() { return SecurityUtils.getCurrentUserLogin().orElseThrow(EnterpriseAccess::denied); }
    public Member member() { return store.member(actor()).filter(m -> m.active).orElseThrow(EnterpriseAccess::denied); }
    public boolean discoverable() { return store.member(actor()).map(m -> m.active).orElse(false); }
    public void lockForWrite() {
        var member = member(); String tenant = member.tenant;
        if (store.lock(tenant) == null) throw denied();
        store.refresh(member);
        if (!member.active || !member.tenant.equals(tenant)) throw denied();
    }
    public Project project(Member member, String code) {
        if (!member.projectSet().contains(code)) throw denied();
        return store.project(member.tenant, code).orElseThrow(EnterpriseAccess::denied);
    }
    public List<Delegation> activeDelegations(Member receiver) {
        var now = clock.instant();
        return store.delegations(receiver.tenant).stream().filter(d -> d.delegate.equals(receiver.login) && d.revokedAt == null &&
            !now.isBefore(d.validFrom) && now.isBefore(d.validUntil) && receiver.projectSet().contains(d.project)).filter(d -> {
                var grantor = store.member(d.grantor).orElse(null);
                var project = store.project(receiver.tenant, d.project).orElse(null);
                return grantor != null && grantor.active && grantor.tenant.equals(receiver.tenant) && grantor.has("MANAGER") &&
                    grantor.projectSet().contains(d.project) && project != null && project.department.equals(grantor.department);
            }).toList();
    }
    public Specification<FdeObject> scope() {
        var member = member();
        var delegated = activeDelegations(member).stream().map(d -> d.project).collect(Collectors.toSet());
        return (root, query, cb) -> {
            var scope = cb.equal(root.get("owner"), member.login);
            if (member.has("MANAGER")) scope = cb.or(scope, cb.equal(root.get("department"), member.department));
            if (member.has("FINANCE")) scope = cb.conjunction();
            if (!delegated.isEmpty()) scope = cb.or(scope, root.get("project").in(delegated));
            return cb.and(cb.equal(root.get("tenant"), member.tenant),
                member.projectSet().isEmpty() ? cb.disjunction() : root.get("project").in(member.projectSet()), scope);
        };
    }
    public Map<String, Object> prepareCreate(Map<String, Object> data) {
        var member = member(); var project = project(member, data.get("project").toString());
        var result = new LinkedHashMap<>(data); result.put("tenant", member.tenant); result.put("department", project.department);
        return result;
    }
    public void stamp(FdeObject object, Map<String, Object> data) {
        object.tenant = data.get("tenant").toString(); object.department = data.get("department").toString(); object.project = data.get("project").toString();
    }
    public String searchText(Map<String, Object> data) {
        return data.entrySet().stream().filter(e -> !e.getKey().equals("internalNote")).map(e -> String.valueOf(e.getValue())).collect(Collectors.joining(" "));
    }
    public String businessScope(Map<String, Object> data) { return data.get("tenant").toString(); }
    public boolean canAct(FdeObject object, String action) {
        var member = member();
        if (!member.tenant.equals(object.tenant) || !member.projectSet().contains(object.project)) return false;
        if (action.equals("submit")) return object.owner.equals(member.login);
        if (object.owner.equals(member.login)) return false;
        if (action.equals("approve-finance") || action.equals("reject") && object.state.equals("FINANCE_PENDING")) {
            var reviewer = mapper.readTree(object.payload).path("departmentApprover");
            return member.has("FINANCE") && reviewer.isTextual() && !member.login.equals(reviewer.asText());
        }
        if (!Set.of("approve-department", "reject").contains(action)) return false;
        if (member.has("MANAGER") && member.department.equals(object.department)) return true;
        return activeDelegations(member).stream().anyMatch(d -> d.project.equals(object.project) && !d.grantor.equals(object.owner));
    }
    public Set<String> visibleFields(FdeSchema.ObjectType type, FdeObject object) {
        var member = member(); var fields = new HashSet<>(type.fields().keySet());
        boolean reviewer = member.has("FINANCE") || member.has("MANAGER") && (object == null || member.department.equals(object.department)) ||
            activeDelegations(member).stream().anyMatch(d -> object == null || d.project.equals(object.project));
        if (!reviewer) fields.remove("internalNote");
        return fields;
    }
}
