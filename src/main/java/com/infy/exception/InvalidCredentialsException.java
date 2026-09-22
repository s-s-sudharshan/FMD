package com.infy.exception;

/**
 * Thrown on a failed login (unknown username or wrong password) and on a
 * failed "current password" check during Change Password. Maps to 401.
 * Deliberately kept distinct from UserDeactivatedException so the two SRS
 * messages ("Wrong username or password" vs "User is Deactivated") never
 * collapse into one.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
