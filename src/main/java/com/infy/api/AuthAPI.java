package com.infy.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.ApiResponseDto;
import com.infy.dto.ForgotPasswordStep1RequestDto;
import com.infy.dto.ForgotPasswordStep2RequestDto;
import com.infy.dto.LoginRequestDto;
import com.infy.dto.LoginResponseDto;
import com.infy.dto.ResetPasswordRequestDto;
import com.infy.enums.SecretQuestion;
import com.infy.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * FE US01 (Login), US08 (Logout), US20/US21 (Forgot Password). Change
 * Password (US09) is exposed from UserAPI instead -- see that class's
 * javadoc for why.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthAPI {

    private static final Logger logger = LoggerFactory.getLogger(AuthAPI.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private Environment environment;

    /**
     * plan.md Section 8/9's "frontend fires a lightweight GET on app load to
     * receive the XSRF-TOKEN cookie" contract. Injecting CsrfToken as a
     * method parameter forces Spring Security to resolve/render the token,
     * which is what makes CookieCsrfTokenRepository actually write the
     * XSRF-TOKEN cookie on the response. Public (see SecurityConfig) because
     * a caller has no session yet the very first time they call this.
     */
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponseDto<Void>> csrf(CsrfToken csrfToken) {
        // Touching csrfToken.getToken() is what triggers the deferred save.
        csrfToken.getToken();
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .success(true)
                .message(environment.getProperty("API.CSRF_COOKIE_ISSUED", "CSRF cookie issued"))
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request,
                                                                    HttpServletRequest httpRequest,
                                                                    HttpServletResponse httpResponse) {
        logger.info("Received login request for username: {}", request.getUsername());
        LoginResponseDto data = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponseDto.<LoginResponseDto>builder()
                .success(true)
                .message(environment.getProperty("API.LOGIN_SUCCESS", "Login successful"))
                .data(data)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        logger.info("Received logout request");
        authService.logout(httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .success(true)
                .message(environment.getProperty("API.LOGOUT_SUCCESS", "Logout successful"))
                .build());
    }

    @PostMapping("/forgot-password/question")
    public ResponseEntity<ApiResponseDto<SecretQuestion>> getSecretQuestion(@Valid @RequestBody ForgotPasswordStep1RequestDto request) {
        logger.info("Received forgot-password question request for username: {}", request.getUsername());
        SecretQuestion question = authService.getSecretQuestion(request.getUsername());
        return ResponseEntity.ok(ApiResponseDto.<SecretQuestion>builder()
                .success(true)
                .message(environment.getProperty("API.SECRET_QUESTION_RETRIEVED", "Secret question retrieved"))
                .data(question)
                .build());
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<ApiResponseDto<Void>> verifySecretAnswer(@Valid @RequestBody ForgotPasswordStep2RequestDto request,
                                                                     HttpServletRequest httpRequest) {
        logger.info("Received forgot-password verify request for username: {}", request.getUsername());
        authService.verifySecretAnswer(request, httpRequest);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .success(true)
                .message(environment.getProperty("API.SECRET_ANSWER_VERIFIED", "Secret answer verified"))
                .build());
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<ApiResponseDto<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request,
                                                                HttpServletRequest httpRequest) {
        logger.info("Received forgot-password reset request for username: {}", request.getUsername());
        authService.resetPassword(request, httpRequest);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .success(true)
                .message(environment.getProperty("API.PASSWORD_RESET_SUCCESS", "Password reset successful"))
                .build());
    }
}
