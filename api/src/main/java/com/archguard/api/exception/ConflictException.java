package com.archguard.api.exception;

/** Signals a resource exists but is not yet in a state that supports the operation. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
