package com.archguard.api.dto;

/** Live executor measurements combined with durable scan lifecycle counts. */
public record QueueStatusResponse(int activeThreadCount, int poolSize, int queueSize, int queueCapacity,
                                  long queuedScans, long runningScans, int maxConcurrentScans) {
}