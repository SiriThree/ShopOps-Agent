package com.sirithree.shopops.admin.reliability.service;

/** Raised when an existing logical write identity is replayed with a different semantic payload. */
public class IdempotencyConflictException extends IllegalStateException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
