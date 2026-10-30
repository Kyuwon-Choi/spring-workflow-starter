package io.github.jhipster.sample.kit;

import jakarta.persistence.*;

@Entity
@Table(name = "kit_asset")
public class KitAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 32)
    String code;

    @Column(nullable = false, length = 120)
    String name;

    protected KitAsset() {}

    public KitAsset(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
