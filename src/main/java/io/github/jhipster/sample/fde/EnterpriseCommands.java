package io.github.jhipster.sample.fde;

import java.util.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Example workflow adapters: version selection, conditional stages and a shared budget. */
@Configuration
public class EnterpriseCommands {
    private final EnterpriseAccess access;
    public EnterpriseCommands(EnterpriseAccess access) { this.access = access; }
    private static ResponseStatusException conflict(String reason) { return new ResponseStatusException(HttpStatus.CONFLICT, reason); }
    private void spend(Map<String, Object> current) {
        var member = access.member();
        var project = access.project(member, current.get("project").toString());
        long amount = ((Number) current.get("amount")).longValue();
        if (amount > project.budgetLimit - project.spent) throw conflict("Project budget is insufficient");
        project.spent = Math.addExact(project.spent, amount);
        access.store.flush(); // Same transaction/tenant lock as the request and its audit.
    }
    @Bean FdeExtension enterpriseSubmit() {
        return new FdeExtension() {
            public String id() { return "enterprise-submit"; }
            public Map<String, Object> changes(Map<String, Object> current, Map<String, Object> input) {
                var member = access.member(); var now = access.clock.instant();
                var rule = access.store.ruleAt(member.tenant, now).orElseThrow(() -> conflict("No effective approval policy"));
                var result = new LinkedHashMap<String, Object>();
                result.put("policyVersion", rule.key); result.put("submittedAt", now.toString());
                for (var field : List.of("departmentApprover", "finalApprover", "reason", "internalNote")) result.put(field, null);
                return result;
            }
        };
    }
    @Bean FdeExtension enterpriseDepartmentApproval() {
        return new FdeExtension() {
            public String id() { return "enterprise-department"; }
            public Map<String, Object> changes(Map<String, Object> current, Map<String, Object> input) { return Map.of(); }
            public Outcome apply(Map<String, Object> current, Map<String, Object> input, String target) {
                var member = access.member();
                var rule = access.store.rule(member.tenant, Objects.toString(current.get("policyVersion"), ""))
                    .orElseThrow(() -> conflict("Submitted approval policy is unavailable"));
                var result = new LinkedHashMap<String, Object>(); result.put("departmentApprover", member.login);
                long amount = ((Number) current.get("amount")).longValue();
                if (rule.financeAbove != null && amount > rule.financeAbove) return new Outcome(result, "FINANCE_PENDING");
                spend(current); result.put("finalApprover", member.login); return new Outcome(result, "APPROVED");
            }
        };
    }
    @Bean FdeExtension enterpriseFinanceApproval() {
        return new FdeExtension() {
            public String id() { return "enterprise-finance"; }
            public Map<String, Object> changes(Map<String, Object> current, Map<String, Object> input) {
                spend(current); return Map.of("finalApprover", access.actor());
            }
        };
    }
}
