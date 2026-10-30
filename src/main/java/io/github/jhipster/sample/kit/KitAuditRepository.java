package io.github.jhipster.sample.kit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KitAuditRepository extends JpaRepository<KitAuditEvent, Long> {
    List<KitAuditEvent> findByRequestIdOrderByIdAsc(Long requestId);
}
