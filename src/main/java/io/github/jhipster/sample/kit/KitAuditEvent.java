package io.github.jhipster.sample.kit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "kit_audit_event")
public class KitAuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    KitRequest request;

    @Column(nullable = false, length = 50)
    String actor;

    @Column(nullable = false, length = 24)
    String action;

    @Column(length = 20)
    String fromState;

    @Column(nullable = false, length = 20)
    String toState;

    @Column(nullable = false)
    Instant occurredAt;

    protected KitAuditEvent() {}

    public KitAuditEvent(KitRequest request, String actor, String action, KitRequest.State from) {
        this.request = request;
        this.actor = actor;
        this.action = action;
        this.fromState = from == null ? null : from.name();
        this.toState = request.state.name();
        this.occurredAt = Instant.now();
    }
}
