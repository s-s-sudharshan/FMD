package com.infy.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.infy.dto.ErrorResponseDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Central exception -> HTTP status mapping (plan.md Section 7), extended
 * with the two Phase 2 exceptions and the AccessDeniedException fix below.
 *
 * P3 fix (phase2_review_actions.md): @PreAuthorize throws AccessDeniedException
 * during controller-method invocation, which this @RestControllerAdvice
 * intercepts before it can reach SecurityConfig's registered
 * RestAccessDeniedHandler (that handler only sees denials the filter chain
 * itself raises, e.g. from authorizeHttpRequests rules -- not from method
 * security). Without an explicit handler here, it fell through to
 * @ExceptionHandler(Exception.class) and returned 500 instead of 403. Reuses
 * the same General.ACCESS_DENIED_MESSAGE key RestAccessDeniedHandler uses, so
 * the message is identical whichever path catches the denial.
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

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateUsername(DuplicateUsernameException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Duplicate username on add user: {}", message);
        return buildResponse(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler(InvalidRoleChangeException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidRoleChange(InvalidRoleChangeException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Invalid role change: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String message = environment.getProperty("General.ACCESS_DENIED_MESSAGE",
                "You do not have permission to perform this action");
        logger.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
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

    private String resolveMessage(RuntimeException ex) {
        return environment.getProperty(ex.getMessage(), ex.getMessage());
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponseDto body = ErrorResponseFactory.build(status, message, request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }
}
