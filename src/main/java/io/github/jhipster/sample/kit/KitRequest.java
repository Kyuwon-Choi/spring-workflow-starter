package io.github.jhipster.sample.kit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "kit_request")
public class KitRequest {
    public enum State { OPEN, IN_PROGRESS, DONE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    KitAsset asset;

    @Column(nullable = false, length = 160)
    String title;

    @Column(nullable = false, length = 50)
    String ownerLogin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    State state;

    @Version
    Long version;

    @Column(nullable = false)
    Instant createdAt;

    protected KitRequest() {}

    public KitRequest(KitAsset asset, String title, String ownerLogin) {
        this.asset = asset;
        this.title = title;
        this.ownerLogin = ownerLogin;
        this.state = State.OPEN;
        this.createdAt = Instant.now();
    }
}
