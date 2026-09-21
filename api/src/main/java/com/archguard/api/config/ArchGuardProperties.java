package com.archguard.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Bounded, environment-configurable limits for repository ingestion and scan execution. */
@ConfigurationProperties(prefix = "archguard")
public class ArchGuardProperties {
    private final Ingestion ingestion = new Ingestion();
    private final Scan scan = new Scan();

    public Ingestion getIngestion() { return ingestion; }
    public Scan getScan() { return scan; }

    public static class Ingestion {
        private long maxCloneBytes = 104857600;
        private Duration cloneTimeout = Duration.ofSeconds(60);
        public long getMaxCloneBytes() { return maxCloneBytes; }
        public void setMaxCloneBytes(long maxCloneBytes) { this.maxCloneBytes = maxCloneBytes; }
        public Duration getCloneTimeout() { return cloneTimeout; }
        public void setCloneTimeout(Duration cloneTimeout) { this.cloneTimeout = cloneTimeout; }
    }

    public static class Scan {
        private boolean localEnabled;
        private int corePoolSize = 2;
        private int maxPoolSize = 4;
        private int queueCapacity = 20;
        public boolean isLocalEnabled() { return localEnabled; }
        public void setLocalEnabled(boolean localEnabled) { this.localEnabled = localEnabled; }
        public int getCorePoolSize() { return corePoolSize; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
    }
}
