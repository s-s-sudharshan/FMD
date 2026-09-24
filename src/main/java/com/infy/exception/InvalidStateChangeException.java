package com.infy.exception;

/**
 * Thrown when a user/device is asked to move into a state it is already in
 * (e.g. activating an already ACTIVATED user or device). Not named in
 * plan.md Section 7, added following the same one-exception-per-failure
 * pattern as InvalidRoleChangeException. Maps to 400.
 */
public class InvalidStateChangeException extends RuntimeException {
    public InvalidStateChangeException(String message) {
        super(message);
    }
}
