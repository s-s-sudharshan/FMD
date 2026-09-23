package com.infy.exception;

/**
 * Thrown when Add User (BE US03) is called with a username that already
 * exists. Maps to 409 — same conflict semantics reserved for Device's
 * DuplicateDeviceException in a later phase.
 */
public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String message) {
        super(message);
    }
}
