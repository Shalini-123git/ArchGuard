package com.archguard.api.exception;

/** Signals a requested persisted resource does not exist. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
