package com.archguard.analysis;

/** Signals that the caller cancelled an in-progress analysis. */
public class ScanCancelledException extends RuntimeException {
    public ScanCancelledException() {
        super("Scan cancelled");
    }
}
