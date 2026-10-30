package io.github.jhipster.sample.kit;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface KitRequestRepository extends JpaRepository<KitRequest, Long> {
    Page<KitRequest> findByOwnerLogin(String ownerLogin, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from KitRequest r where r.id = :id")
    Optional<KitRequest> findForUpdate(@Param("id") Long id);
}
