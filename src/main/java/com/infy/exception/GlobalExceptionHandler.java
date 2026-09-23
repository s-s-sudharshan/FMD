package com.infy.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.infy.dto.ErrorResponseDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Central exception -> HTTP status mapping (plan.md Section 7). Only the
 * exceptions relevant to Phase 1 (Auth) are handled here; later phases add
 * their own @ExceptionHandler methods to this same class rather than
 * duplicating the response-building logic.
 *
 * Custom exceptions now carry a message-property key (e.g. "Service.USER_NOT_FOUND")
 * rather than literal text, so {@link #resolveMessage(RuntimeException)} looks that
 * key up in application.properties via the injected Environment, falling back to the
 * exception's own message if no matching property exists (covers exceptions -- such
 * as UnauthorizedActionException from CurrentUserResolver -- that were left carrying
 * literal text since they're outside this refactor's scope).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private Environment environment;

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("User not found: {}", message);
        return buildResponse(HttpStatus.NOT_FOUND, message, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Invalid credentials: {}", message);
        return buildResponse(HttpStatus.UNAUTHORIZED, message, request);
    }

    @ExceptionHandler(UserDeactivatedException.class)
    public ResponseEntity<ErrorResponseDto> handleUserDeactivated(UserDeactivatedException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Deactivated user attempted an action: {}", message);
        return buildResponse(HttpStatus.FORBIDDEN, message, request);
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<ErrorResponseDto> handleWeakPassword(WeakPasswordException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Weak password rejected: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handlePasswordMismatch(PasswordMismatchException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Password mismatch: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(InvalidSecretAnswerException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidSecretAnswer(InvalidSecretAnswerException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Invalid secret answer / forgot-password step ordering: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponseDto> handleUnauthorizedAction(UnauthorizedActionException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Unauthorized action: {}", message);
        return buildResponse(HttpStatus.FORBIDDEN, message, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        logger.warn("Request validation failed: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        logger.warn("Constraint violation: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception ex, HttpServletRequest request) {
        logger.error("Unhandled exception on {}", request.getRequestURI(), ex);
        String message = environment.getProperty("General.EXCEPTION_MESSAGE", "An unexpected error occurred");
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request);
    }

    /**
     * Resolves a custom exception's message as a property key against
     * application.properties (e.g. "Service.USER_NOT_FOUND" -> "User not found.").
     * Falls back to the exception's own message when no property matches --
     * this also correctly handles the already-formatted WeakPasswordException
     * message (built with MessageFormat in AuthServiceImpl), since that text
     * simply won't match any property key and is returned as-is.
     */
    private String resolveMessage(RuntimeException ex) {
        return environment.getProperty(ex.getMessage(), ex.getMessage());
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponseDto body = ErrorResponseFactory.build(status, message, request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }
}
