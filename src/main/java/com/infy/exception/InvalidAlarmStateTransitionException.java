package com.infy.exception;

/**
 * Thrown when an acknowledge/clear/terminate is attempted from a status that
 * doesn't allow it (e.g. clearing an unacknowledged alarm). Maps to 400.
 */
public class InvalidAlarmStateTransitionException extends RuntimeException {
    public InvalidAlarmStateTransitionException(String message) {
        super(message);
    }
}
