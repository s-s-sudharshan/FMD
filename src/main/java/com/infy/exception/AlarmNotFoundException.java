package com.infy.exception;

/** Thrown when no Alarm matches a supplied id (or, for bulk, when any id is unknown). Maps to 404. */
public class AlarmNotFoundException extends RuntimeException {
    public AlarmNotFoundException(String message) {
        super(message);
    }
}
