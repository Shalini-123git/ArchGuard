package com.archguard.api.service;

import com.archguard.api.config.ArchGuardProperties;
import com.archguard.api.exception.BadRequestException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Function;

/** Safely acquires a shallow HTTPS clone and removes its untrusted temporary directory after use. */
@Service
public class IngestionService {
    private final ArchGuardProperties properties;
    private final ScanCancellationRegistry cancellationRegistry;

    @Autowired
    public IngestionService(ArchGuardProperties properties, ScanCancellationRegistry cancellationRegistry) {
        this.properties = properties;
        this.cancellationRegistry = cancellationRegistry;
    }

    public IngestionService(ArchGuardProperties properties) {
        this(properties, new ScanCancellationRegistry());
    }

    public <T> T withRemoteRepository(String repositoryUrl, Function<ProjectSource, T> operation) {
        validateRemoteUrl(repositoryUrl);
        Path tempDirectory = null;
        try {
            tempDirectory = Files.createTempDirectory("archguard-clone-");
            try (Git git = Git.cloneRepository()
                    .setURI(repositoryUrl)
                    .setDirectory(tempDirectory.toFile())
                    .setDepth(1)
                    .setCloneAllBranches(false)
                    .setTimeout(timeoutSeconds())
                    .setProgressMonitor(new ProgressMonitor() {
                        public void start(int totalTasks) { }
                        public void beginTask(String title, int totalWork) { }
                        public void update(int completed) { }
                        public void endTask() { }
                        public void showDuration(boolean enabled) { }
                        public boolean isCancelled() { var id = currentScanId(); return id != null && cancellationRegistry.isCancelled(id); }
                    })
                    .call()) {
                enforceCloneSize(tempDirectory);
                String commitSha = git.getRepository().resolve(Constants.HEAD).name();
                return operation.apply(new ProjectSource(tempDirectory, commitSha));
            }

        } catch (IOException exception) {
            throw new BadRequestException("Could not prepare repository scan: " + exception.getMessage());
        } catch (Exception exception) {
            if (exception instanceof BadRequestException badRequestException) {
                throw badRequestException;
            }
            throw new BadRequestException("Could not clone repository: " + exception.getMessage());
        } finally {
            deleteTemporaryDirectory(tempDirectory);
        }
    }

    private java.util.UUID currentScanId() {
        String value = org.slf4j.MDC.get("scanId");
        return value == null ? null : java.util.UUID.fromString(value);
    }

    public void validateRemoteUrl(String repositoryUrl) {
        validateHttpsUrl(repositoryUrl);
    }

    public ProjectSource localSource(String localPath) {
        if (!properties.getScan().isLocalEnabled()) {
            throw new BadRequestException("Local scans are disabled");
        }
        Path path = Path.of(localPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            throw new BadRequestException("Local scan path is not a directory");
        }
        return new ProjectSource(path, null);
    }

    private void validateHttpsUrl(String repositoryUrl) {
        try {
            URI uri = new URI(repositoryUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new BadRequestException("repoUrl must be an HTTPS repository URL");
            }
        } catch (URISyntaxException exception) {
            throw new BadRequestException("repoUrl must be a valid HTTPS repository URL");
        }
    }

    private int timeoutSeconds() {
        long seconds = Math.max(1, properties.getIngestion().getCloneTimeout().toSeconds());
        return Math.toIntExact(Math.min(seconds, Integer.MAX_VALUE));
    }

    private void enforceCloneSize(Path directory) throws IOException {
        long totalBytes;
        try (var files = Files.walk(directory)) {
            totalBytes = files.filter(Files::isRegularFile).mapToLong(this::fileSize).sum();
        }
        if (totalBytes > properties.getIngestion().getMaxCloneBytes()) {
            throw new BadRequestException("Cloned repository exceeds the configured size limit");
        }
    }

    private long fileSize(Path path) {
        try { return Files.size(path); } catch (IOException exception) { throw new IllegalStateException(exception); }
    }

    private void deleteTemporaryDirectory(Path directory) {
        if (directory == null) return;
        try (var files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) {
            // A failed cleanup must not hide the scan result; the directory contains no persisted source data.
        }
    }
}
