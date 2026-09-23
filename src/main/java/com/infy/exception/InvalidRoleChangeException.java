package com.infy.exception;

/**
 * Thrown when Change User Role (BE US04) is called with a newRole equal to
 * the user's current role. Not explicitly named in plan.md Section 7's
 * exception list, but that section maps every 400-class validation failure
 * to its own exception type rather than a generic one, so this follows the
 * same pattern (flagged in tasks/lessons.md). Maps to 400.
 */
public class InvalidRoleChangeException extends RuntimeException {
    public InvalidRoleChangeException(String message) {
        super(message);
    }
}
