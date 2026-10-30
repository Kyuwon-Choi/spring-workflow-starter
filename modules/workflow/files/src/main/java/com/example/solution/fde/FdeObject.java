package com.example.solution.fde;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fde_object", uniqueConstraints = @UniqueConstraint(columnNames = {"domain_id", "object_type", "business_scope", "business_key"}))
public class FdeObject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "domain_id", nullable = false, length = 64) String domain;
    @Column(name = "object_type", nullable = false, length = 64) String objectType;
    @Column(name = "business_key", length = 255) String businessKey;
    @Column(name = "business_scope", nullable = false, length = 64) String businessScope = "";
    @Lob @Column(nullable = false) String payload;
    @Column(nullable = false, length = 64) String state;
    @Column(nullable = false, length = 50) String owner;
    @Version Long version;
    @Column(nullable = false) Instant createdAt;
    @Column(name = "tenant_id", length = 64) String tenant;
    @Column(name = "department_id", length = 64) String department;
    @Column(name = "project_id", length = 64) String project;
    @Lob @Column(name = "public_search") String publicSearch;
    protected FdeObject() {}
}
