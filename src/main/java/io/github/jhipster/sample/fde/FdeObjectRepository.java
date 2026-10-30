package io.github.jhipster.sample.fde;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface FdeObjectRepository extends JpaRepository<FdeObject, Long>, JpaSpecificationExecutor<FdeObject> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from FdeObject o where o.id = :id")
    Optional<FdeObject> locked(@Param("id") Long id);
    boolean existsByDomainAndObjectTypeAndBusinessScopeAndBusinessKey(String domain, String objectType, String businessScope, String businessKey);
}
