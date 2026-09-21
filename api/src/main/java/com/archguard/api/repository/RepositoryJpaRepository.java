package com.archguard.api.repository;

import com.archguard.api.persistence.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Database access for scanned source repositories. */
public interface RepositoryJpaRepository extends JpaRepository<RepositoryEntity, UUID> {
    Optional<RepositoryEntity> findByRepositoryUrl(String repositoryUrl);
}
