package com.infy.exception;

/**
 * Thrown when a newPassword/confirmPassword pair (Change Password or Reset
 * Password) don't match. Maps to 400.
 */
public class PasswordMismatchException extends RuntimeException {
    public PasswordMismatchException(String message) {
        super(message);
    }
}
