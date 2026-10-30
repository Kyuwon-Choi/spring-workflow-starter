package io.github.jhipster.sample.fde;

import static io.github.jhipster.sample.fde.EnterpriseRecords.*;
import io.github.jhipster.sample.domain.User;
import io.github.jhipster.sample.repository.AuthorityRepository;
import io.github.jhipster.sample.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Optional, local practice-only identities. Never enabled by default. */
@Component @Profile("practice") @ConditionalOnProperty(name = "fde.enterprise.demo.enabled", havingValue = "true")
public class EnterpriseDemoData implements ApplicationRunner {
    final EnterpriseStore store; final UserRepository users; final AuthorityRepository authorities; final PasswordEncoder passwords;
    public EnterpriseDemoData(EnterpriseStore store, UserRepository users, AuthorityRepository authorities, PasswordEncoder passwords) {
        this.store = store; this.users = users; this.authorities = authorities; this.passwords = passwords;
    }
    @Transactional public void run(ApplicationArguments args) {
        if (store.member("admin").isPresent()) return;
        for (var login : List.of("fde-manager", "fde-finance", "fde-delegate", "fde-outsider")) {
            if (users.findOneByLogin(login).isEmpty()) {
                var user = new User(); user.setLogin(login); user.setPassword(passwords.encode("practicepass")); user.setEmail(login + "@example.invalid");
                user.setActivated(true); user.setLangKey("en"); user.setCreatedBy("demo-bootstrap");
                user.setAuthorities(Set.of(authorities.findById("ROLE_USER").orElseThrow())); users.saveAndFlush(user);
            }
        }
        for (var id : List.of("DEMO-A", "DEMO-B")) {
            var tenant = new Tenant(); tenant.id = id; store.save(tenant);
            var rule = new Rule(); rule.tenant = id; rule.key = "V1"; rule.effectiveAt = Instant.EPOCH; store.save(rule);
        }
        for (var tuple : List.of(List.of("DEMO-A", "P-A", "D-A"), List.of("DEMO-A", "P-B", "D-B"), List.of("DEMO-B", "P-A", "D-A"))) {
            var project = new Project(); project.tenant = tuple.get(0); project.code = tuple.get(1); project.department = tuple.get(2); project.budgetLimit = 10000000; store.save(project);
        }
        add("admin", "DEMO-A", "D-A", "ORG_ADMIN,MANAGER", "P-A,P-B"); add("user", "DEMO-A", "D-A", "STAFF", "P-A");
        add("fde-manager", "DEMO-A", "D-A", "MANAGER", "P-A"); add("fde-finance", "DEMO-A", "D-B", "FINANCE", "P-A,P-B");
        add("fde-delegate", "DEMO-A", "D-A", "STAFF", "P-A"); add("fde-outsider", "DEMO-B", "D-A", "ORG_ADMIN,MANAGER", "P-A");
    }
    void add(String login, String tenant, String department, String roles, String projects) {
        var member = new Member(); member.login = login; member.tenant = tenant; member.department = department; member.roles = roles; member.projects = projects; store.save(member);
    }
}
