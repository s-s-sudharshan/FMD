package com.infy.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import com.infy.dto.ErrorResponseDto;

/**
 * Single place that builds an {@link ErrorResponseDto}. Used by
 * {@link GlobalExceptionHandler} (for exceptions thrown inside a controller)
 * and by {@code RestAuthenticationEntryPoint} / {@code RestAccessDeniedHandler}
 * (for failures Spring Security's filter chain raises before a controller is
 * ever reached) so both paths produce the exact same JSON shape -- see codex
 * review finding #2.
 */
public final class ErrorResponseFactory {

    private ErrorResponseFactory() {
    }

    public static ErrorResponseDto build(HttpStatus status, String message, String path) {
        return ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();
    }
}
