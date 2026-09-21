package com.archguard.api.exception;

/** Signals a client request that is syntactically valid but not allowed by ArchGuard policy. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
