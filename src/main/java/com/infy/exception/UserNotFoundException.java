package com.infy.exception;

/** Thrown when a lookup by username/id finds no matching User. Maps to 404. */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
