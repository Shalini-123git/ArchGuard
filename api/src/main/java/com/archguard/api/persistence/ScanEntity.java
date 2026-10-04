package com.archguard.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Persisted execution and outcome of one architecture scan. */
@Entity
@Table(name = "scans")
public class ScanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private RepositoryEntity repository;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private ScanSourceType sourceType;

    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    @Column(name = "rules_yaml", columnDefinition = "TEXT")
    private String rulesYaml;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScanStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ScanEntity() {
    }

    public ScanEntity(RepositoryEntity repository, ScanSourceType sourceType, String rulesYaml) {
        this.repository = repository;
        this.sourceType = sourceType;
        this.rulesYaml = rulesYaml;
        this.status = ScanStatus.QUEUED;
        this.createdAt = Instant.now();
    }

    public void markRunning() { status = ScanStatus.RUNNING; startedAt = Instant.now(); }
    public void markCompleted(String commitSha) { this.commitSha = commitSha; status = ScanStatus.COMPLETED; completedAt = Instant.now(); }
    public void markFailed(String message) { status = ScanStatus.FAILED; errorMessage = message; completedAt = Instant.now(); }
    public void markCancelled() { status = ScanStatus.CANCELLED; errorMessage = "Cancelled by user"; completedAt = Instant.now(); }

    public UUID getId() { return id; }
    public RepositoryEntity getRepository() { return repository; }
    public ScanSourceType getSourceType() { return sourceType; }
    public String getCommitSha() { return commitSha; }
    public String getRulesYaml() { return rulesYaml; }
    public ScanStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
