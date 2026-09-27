package com.archguard.api.repository;

import com.archguard.api.persistence.ExplanationCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database cache for successful and deterministic fallback explanations. */
public interface ExplanationCacheRepository extends JpaRepository<ExplanationCacheEntity, String> { }
