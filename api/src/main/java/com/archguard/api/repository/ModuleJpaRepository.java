package com.archguard.api.repository;

import com.archguard.api.persistence.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Database access for persisted package nodes. */
public interface ModuleJpaRepository extends JpaRepository<ModuleEntity, UUID> {
    List<ModuleEntity> findByScanIdOrderByName(UUID scanId);
    void deleteByScanId(UUID scanId);
}
