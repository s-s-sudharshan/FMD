package com.infy.exception;

/**
 * Thrown when a supplied password fails the minimum-strength rule (SRS: fewer
 * than 4 characters is rejected outright; 4+ characters is accepted and
 * merely classified weak/medium/strong). Maps to 400.
 */
public class WeakPasswordException extends RuntimeException {
    public WeakPasswordException(String message) {
        super(message);
    }
}
