package com.infy.exception;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.infy.dto.ErrorResponseDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Central exception -> HTTP status mapping (plan.md Section 7).
 *
 * Handlers beyond the plan, each closing a path that would otherwise reach
 * the catch-all and return 500:
 *  - AccessDeniedException: @PreAuthorize denials are thrown during controller
 *    invocation, so this advice sees them before SecurityConfig's
 *    RestAccessDeniedHandler can (403).
 *  - BindException (covers MethodArgumentNotValidException, its subtype):
 *    bean-validation failures and bad enum/number values in a bean bound from
 *    the query string, e.g. GET /api/alarms?severity=FOO (400).
 *  - MethodArgumentTypeMismatchException: bad single @RequestParam, e.g.
 *    GET /api/devices?state=FOO (400).
 *  - HttpMessageNotReadableException: missing/malformed JSON body or an
 *    invalid enum inside it (400).
 *  - InvalidStateChangeException: reactivating something already active (400).
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

    @ExceptionHandler(InvalidStateChangeException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidStateChange(InvalidStateChangeException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Invalid state change: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(DeviceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleDeviceNotFound(DeviceNotFoundException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Device not found: {}", message);
        return buildResponse(HttpStatus.NOT_FOUND, message, request);
    }

    @ExceptionHandler(DuplicateDeviceException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateDevice(DuplicateDeviceException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Duplicate device: {}", message);
        return buildResponse(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler(InvalidIpAddressException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidIpAddress(InvalidIpAddressException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Invalid IP address: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(AlarmNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleAlarmNotFound(AlarmNotFoundException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Alarm not found: {}", message);
        return buildResponse(HttpStatus.NOT_FOUND, message, request);
    }

    @ExceptionHandler(InvalidAlarmStateTransitionException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidAlarmStateTransition(InvalidAlarmStateTransitionException ex, HttpServletRequest request) {
        String message = resolveMessage(ex);
        logger.warn("Invalid alarm state transition: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String message = environment.getProperty("General.ACCESS_DENIED_MESSAGE",
                "You do not have permission to perform this action");
        logger.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, message, request);
    }

    /**
     * Any bean-binding failure. Conversion failures (bad enum/number) read
     * "Invalid value 'X' for parameter 'f'. Allowed values: [...]"; validation
     * failures (@NotBlank, @NotNull, ...) keep the "field: message" form.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponseDto> handleBind(BindException ex, HttpServletRequest request) {
        BindingResult result = ex.getBindingResult();
        String message = result.getFieldErrors().stream()
                .map(fe -> fe.isBindingFailure()
                        ? invalidValueMessage(fe.getField(), fe.getRejectedValue(), result.getFieldType(fe.getField()))
                        : fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Invalid request";
        }
        logger.warn("Request binding/validation failed on {}: {}", request.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    /** Bad value for a single @RequestParam / path variable, e.g. ?state=FOO. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = invalidValueMessage(ex.getName(), ex.getValue(), ex.getRequiredType());
        logger.warn("Parameter type mismatch on {}: {}", request.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    /** JSON body missing, malformed, or containing an invalid enum value. Parser detail goes to the log only. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Malformed request body";
        if (ex.getMessage() != null && ex.getMessage().startsWith("Required request body is missing")) {
            message = "Request body is missing";
        } else if (ex.getCause() instanceof InvalidFormatException ife
                && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
            String field = ife.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));
            message = invalidValueMessage(field, ife.getValue(), ife.getTargetType());
        }
        logger.warn("Unreadable request body on {}: {} | cause: {}",
                request.getRequestURI(), message, ex.getMostSpecificCause().getMessage());
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

    private String invalidValueMessage(String name, Object value, Class<?> requiredType) {
        String message = "Invalid value '" + value + "' for parameter '" + name + "'";
        if (requiredType != null && requiredType.isEnum()) {
            message += ". Allowed values: " + Arrays.toString(requiredType.getEnumConstants());
        }
        return message;
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponseDto body = ErrorResponseFactory.build(status, message, request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }
}
