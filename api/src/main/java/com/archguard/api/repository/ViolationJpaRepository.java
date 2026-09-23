package com.archguard.api.repository;

import com.archguard.api.persistence.ViolationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/** Database access for persisted architecture violations. */
public interface ViolationJpaRepository extends JpaRepository<ViolationEntity, UUID> {
    @Query("select distinct v from ViolationEntity v left join fetch v.affectedModules where v.scan.id = :scanId")
    List<ViolationEntity> findWithAffectedModulesByScanId(UUID scanId);
}
