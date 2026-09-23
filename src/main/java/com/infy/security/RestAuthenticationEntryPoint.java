package com.infy.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.dto.ErrorResponseDto;
import com.infy.exception.ErrorResponseFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles requests Spring Security itself rejects for lack of authentication
 * -- i.e. failures that never reach a controller, so {@code
 * GlobalExceptionHandler}'s {@code @ExceptionHandler} methods never see them
 * (codex review finding #2). Without this, an unauthenticated call to a
 * protected endpoint fell back to Spring Security's default HTML/plain-text
 * 403 response instead of the planned {@link ErrorResponseDto} JSON shape at
 * 401. Registered via {@code SecurityConfig}'s {@code exceptionHandling()}.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment environment;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        String message = environment.getProperty("General.UNAUTHORIZED_MESSAGE",
                "Authentication is required to access this resource");
        ErrorResponseDto body = ErrorResponseFactory.build(
                HttpStatus.UNAUTHORIZED,
                message,
                request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
