package com.infy.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.dto.ErrorResponseDto;
import com.infy.exception.ErrorResponseFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles requests Spring Security rejects because an authenticated user
 * lacks the required role/authority (e.g. a future @PreAuthorize check) --
 * the counterpart to {@link RestAuthenticationEntryPoint} for the
 * "authenticated but forbidden" case (codex review finding #2). Without
 * this, method-security denials fell back to Spring Security's default 403
 * response instead of the planned {@link ErrorResponseDto} JSON shape.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment environment;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        String message = environment.getProperty("General.ACCESS_DENIED_MESSAGE",
                "You do not have permission to perform this action");
        ErrorResponseDto body = ErrorResponseFactory.build(
                HttpStatus.FORBIDDEN,
                message,
                request.getRequestURI());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
