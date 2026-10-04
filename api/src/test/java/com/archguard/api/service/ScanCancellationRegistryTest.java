package com.archguard.api.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ScanCancellationRegistryTest {
    @Test
    void cancellationInterruptsRegisteredThreadAndRemainsVisibleUntilUnregister() {
        ScanCancellationRegistry registry = new ScanCancellationRegistry();
        UUID scanId = UUID.randomUUID();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException exception) {
                interrupted.set(true);
            }
        });
        registry.register(scanId, worker);
        worker.start();

        registry.cancel(scanId);

        assertTrue(registry.isCancelled(scanId));
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(1), () -> worker.join());
        assertTrue(interrupted.get());
        registry.unregister(scanId);
        assertFalse(registry.isCancelled(scanId));
    }
}
