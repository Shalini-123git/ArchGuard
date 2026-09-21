package com.archguard.api.repository;

import com.archguard.api.persistence.DependencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Database access for persisted package edges. */
public interface DependencyJpaRepository extends JpaRepository<DependencyEntity, UUID> {
    List<DependencyEntity> findByScanId(UUID scanId);
}
