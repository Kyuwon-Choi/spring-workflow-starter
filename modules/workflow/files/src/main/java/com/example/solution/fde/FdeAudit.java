package com.example.solution.fde;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fde_audit")
public class FdeAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "object_id", nullable = false) FdeObject object;
    @Column(nullable = false, length = 50) String actor;
    @Column(nullable = false, length = 64) String action;
    @Lob String beforeData;
    @Lob @Column(nullable = false) String afterData;
    @Column(length = 64) String beforeState;
    @Column(nullable = false, length = 64) String afterState;
    @Column(nullable = false) Instant occurredAt;
    protected FdeAudit() {}
}
