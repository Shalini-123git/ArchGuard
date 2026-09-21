package com.archguard.api.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Cached provider response for an immutable deterministic violation context. */
@Entity
@Table(name = "explanation_cache")
public class ExplanationCacheEntity {
    @Id private String contextHash;
    @Column(columnDefinition = "TEXT")
    private String explanation;
    private boolean fallback;
    private Instant createdAt;
    protected ExplanationCacheEntity() { }
    public ExplanationCacheEntity(String hash, String explanation, boolean fallback) { this.contextHash = hash; this.explanation = explanation; this.fallback = fallback; this.createdAt = Instant.now(); }
    public String getExplanation() { return explanation; }
    public boolean isFallback() { return fallback; }
}
