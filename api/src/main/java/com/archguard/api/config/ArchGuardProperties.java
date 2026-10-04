package com.archguard.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Bounded, environment-configurable limits for repository ingestion and scan execution. */
@ConfigurationProperties(prefix = "archguard")
public class ArchGuardProperties {
    private final Ingestion ingestion = new Ingestion();
    private final Scan scan = new Scan();
    private final Llm llm = new Llm();

    public Ingestion getIngestion() { return ingestion; }
    public Scan getScan() { return scan; }
    public Llm getLlm() { return llm; }

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

    public static class Llm {
        private String apiKey = "";
        private String model = "llama-3.3-70b-versatile";
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(20);
        private Duration retryBackoff = Duration.ofMillis(500);
        private int maxTokens = 1500;
        private long delayBetweenCallsMs = 400;
        private int blastRadiusLimit = 10;
        private int snippetLineLimit = 3;
        private int violationsPerScanLimit = 20;
        public String getApiKey() { return apiKey; } public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; } public void setModel(String model) { this.model = model; }
        public Duration getConnectTimeout() { return connectTimeout; } public void setConnectTimeout(Duration value) { connectTimeout = value; }
        public Duration getReadTimeout() { return readTimeout; } public void setReadTimeout(Duration value) { readTimeout = value; }
        public Duration getRetryBackoff() { return retryBackoff; } public void setRetryBackoff(Duration value) { retryBackoff = value; }
        public int getMaxTokens() { return maxTokens; } public void setMaxTokens(int value) { maxTokens = value; }
        public long getDelayBetweenCallsMs() { return delayBetweenCallsMs; } public void setDelayBetweenCallsMs(long value) { delayBetweenCallsMs = value; }
        public int getBlastRadiusLimit() { return blastRadiusLimit; } public void setBlastRadiusLimit(int value) { blastRadiusLimit = value; }
        public int getSnippetLineLimit() { return snippetLineLimit; } public void setSnippetLineLimit(int value) { snippetLineLimit = value; }
        public int getViolationsPerScanLimit() { return violationsPerScanLimit; } public void setViolationsPerScanLimit(int value) { violationsPerScanLimit = value; }
    }
}
