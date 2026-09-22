package com.infy.exception;

/**
 * Thrown when an action requires an authenticated principal that isn't
 * present in the SecurityContext (see CurrentUserResolver). Maps to 403.
 */
public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
