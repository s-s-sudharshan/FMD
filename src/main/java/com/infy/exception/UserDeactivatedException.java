package com.infy.exception;

/** Thrown when a DEACTIVATED user attempts to log in. Maps to 403. */
public class UserDeactivatedException extends RuntimeException {
    public UserDeactivatedException(String message) {
        super(message);
    }
}
