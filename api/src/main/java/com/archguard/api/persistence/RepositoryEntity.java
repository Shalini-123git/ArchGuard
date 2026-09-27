package com.archguard.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A source repository or explicitly enabled local source tracked by ArchGuard. */
@Entity
@Table(name = "repositories")
public class RepositoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "repository_url", nullable = false, unique = true, length = 2048)
    private String repositoryUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RepositoryEntity() {
    }

    public RepositoryEntity(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public Instant getCreatedAt() { return createdAt; }
}
