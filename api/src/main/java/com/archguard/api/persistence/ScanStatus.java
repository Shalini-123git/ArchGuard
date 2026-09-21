package com.archguard.api.persistence;

/** Lifecycle states for an asynchronously executed scan. */
public enum ScanStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED
}
