package io.github.jhipster.sample.fde;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FdeAuditRepository extends JpaRepository<FdeAudit, Long> {
    List<FdeAudit> findTop200ByObjectIdOrderByIdDesc(Long objectId);
}
