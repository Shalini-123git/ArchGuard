package com.archguard.api.repository;

import com.archguard.api.persistence.ScanEntity;
import com.archguard.api.persistence.ScanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Database access for scan lifecycle records. */
public interface ScanJpaRepository extends JpaRepository<ScanEntity, UUID> {
    List<ScanEntity> findByRepositoryIdOrderByCreatedAtDesc(UUID repositoryId);
    List<ScanEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<ScanEntity> findByStatusOrderByCreatedAtDesc(ScanStatus status, Pageable pageable);
    List<ScanEntity> findByStatusIn(List<ScanStatus> statuses);
    long countByStatus(ScanStatus status);
}
