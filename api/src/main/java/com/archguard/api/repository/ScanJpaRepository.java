package com.archguard.api.repository;

import com.archguard.api.persistence.ScanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Database access for scan lifecycle records. */
public interface ScanJpaRepository extends JpaRepository<ScanEntity, UUID> {
    List<ScanEntity> findByRepositoryIdOrderByCreatedAtDesc(UUID repositoryId);
}
