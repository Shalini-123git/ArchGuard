package com.archguard.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/** Request to queue a remote repository scan or an explicitly enabled local scan. */
public record CreateScanRequest(
        @Size(max = 2048) String repoUrl,
        @Size(max = 4096) String localPath,
        @Size(max = 1_000_000) String rulesYaml
) {
    @AssertTrue(message = "provide exactly one of repoUrl or localPath")
    public boolean hasOneSource() {
        return (repoUrl != null && !repoUrl.isBlank()) ^ (localPath != null && !localPath.isBlank());
    }
}
