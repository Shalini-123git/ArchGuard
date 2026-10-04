package com.archguard.api.service;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Coordinates persisted cancellation with the currently executing worker thread. */
@Component
public class ScanCancellationRegistry {
    private final ConcurrentMap<UUID, Thread> threads = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Boolean> cancelled = new ConcurrentHashMap<>();

    public void register(UUID scanId, Thread thread) {
        threads.put(scanId, thread);
    }

    public void unregister(UUID scanId) {
        threads.remove(scanId);
        cancelled.remove(scanId);
    }

    public void cancel(UUID scanId) {
        cancelled.put(scanId, Boolean.TRUE);
        Thread thread = threads.get(scanId);
        if (thread != null) thread.interrupt();
    }

    public boolean isCancelled(UUID scanId) {
        return cancelled.containsKey(scanId);
    }
}
