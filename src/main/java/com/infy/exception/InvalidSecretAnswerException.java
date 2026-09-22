package com.infy.exception;

/**
 * Thrown when the Forgot Password flow's step-2 answer doesn't match the
 * stored secretAnswer, or when step 3 (reset) is attempted without a
 * successful step-2 verification first. Maps to 400.
 */
public class InvalidSecretAnswerException extends RuntimeException {
    public InvalidSecretAnswerException(String message) {
        super(message);
    }
}
