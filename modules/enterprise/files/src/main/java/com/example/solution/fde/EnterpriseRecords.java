package com.example.solution.fde;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Relational policy facts are server-managed, never trusted from token business claims. */
public final class EnterpriseRecords {
    private EnterpriseRecords() {}
    public static Set<String> values(String text) {
        return text == null || text.isBlank() ? Set.of() : Arrays.stream(text.split(",")).collect(Collectors.toSet());
    }
    public static String values(Set<String> values) { return values.stream().sorted().collect(Collectors.joining(",")); }
    @Entity(name = "EnterpriseTenant") @Table(name = "fde_tenant")
    public static class Tenant { @Id @Column(length = 64) public String id; public Tenant() {} }
    @Entity(name = "EnterpriseMember") @Table(name = "fde_member")
    public static class Member {
        @Id @Column(length = 50) public String login;
        @Column(nullable = false, length = 64) public String tenant;
        @Column(nullable = false, length = 64) public String department;
        @Column(nullable = false, length = 200) public String roles;
        @Column(nullable = false, length = 4000) public String projects;
        @Column(nullable = false) public boolean active = true;
        @Version public Long version;
        public Member() {}
        public boolean has(String role) { return values(roles).contains(role); }
        public Set<String> projectSet() { return values(projects); }
    }
    @Entity(name = "EnterpriseProject")
    @Table(name = "fde_project", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant", "code"}))
    public static class Project {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(nullable = false, length = 64) public String tenant;
        @Column(nullable = false, length = 64) public String code;
        @Column(nullable = false, length = 64) public String department;
        @Column(name = "budget_limit", nullable = false) public long budgetLimit;
        @Column(nullable = false) public long spent;
        public Project() {}
    }
    @Entity(name = "EnterpriseRule")
    @Table(name = "fde_rule", uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant", "rule_key"}), @UniqueConstraint(columnNames = {"tenant", "effective_at"})})
    public static class Rule {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(nullable = false, length = 64) public String tenant;
        @Column(name = "rule_key", nullable = false, length = 64) public String key;
        @Column(name = "effective_at", nullable = false) public Instant effectiveAt;
        @Column(name = "finance_above") public Long financeAbove;
        public Rule() {}
    }
    @Entity(name = "EnterpriseDelegation") @Table(name = "fde_delegation")
    public static class Delegation {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(nullable = false, length = 64) public String tenant;
        @Column(nullable = false, length = 50) public String grantor;
        @Column(name = "delegate_login", nullable = false, length = 50) public String delegate;
        @Column(nullable = false, length = 64) public String project;
        @Column(name = "valid_from", nullable = false) public Instant validFrom;
        @Column(name = "valid_until", nullable = false) public Instant validUntil;
        @Column(name = "revoked_at") public Instant revokedAt;
        public Delegation() {}
    }
    @Entity(name = "EnterpriseConfigAudit") @Table(name = "fde_config_audit")
    public static class ConfigAudit {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(nullable = false, length = 64) public String tenant;
        @Column(nullable = false, length = 50) public String actor;
        @Column(nullable = false, length = 64) public String action;
        @Lob @Column(name = "before_data") public String beforeData;
        @Lob @Column(name = "after_data", nullable = false) public String afterData;
        @Column(name = "occurred_at", nullable = false) public Instant occurredAt;
        public ConfigAudit() {}
    }
}
