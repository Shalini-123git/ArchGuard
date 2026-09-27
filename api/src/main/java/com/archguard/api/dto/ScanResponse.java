package com.archguard.api.dto;

import com.archguard.api.persistence.ScanEntity;
import com.archguard.api.persistence.ScanStatus;

import java.time.Instant;
import java.util.UUID;

/** Public scan lifecycle representation, intentionally separate from the JPA entity. */
public record ScanResponse(UUID id, UUID repositoryId, String repositoryUrl, String commitSha, ScanStatus status,
                           String errorMessage, Instant createdAt, Instant startedAt, Instant completedAt,
                           Integer healthScore) {
    public static ScanResponse from(ScanEntity scan, Integer healthScore) {
        return new ScanResponse(scan.getId(), scan.getRepository().getId(), scan.getRepository().getRepositoryUrl(),
                scan.getCommitSha(), scan.getStatus(), scan.getErrorMessage(), scan.getCreatedAt(), scan.getStartedAt(),
                scan.getCompletedAt(), healthScore);
    }
}
