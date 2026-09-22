package com.infy.service;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import com.infy.dto.ChangePasswordRequestDto;
import com.infy.dto.ForgotPasswordStep2RequestDto;
import com.infy.dto.LoginRequestDto;
import com.infy.dto.LoginResponseDto;
import com.infy.dto.ResetPasswordRequestDto;
import com.infy.entity.User;
import com.infy.enums.SecretQuestion;
import com.infy.exception.InvalidCredentialsException;
import com.infy.exception.InvalidSecretAnswerException;
import com.infy.exception.PasswordMismatchException;
import com.infy.exception.UserDeactivatedException;
import com.infy.exception.UserNotFoundException;
import com.infy.exception.WeakPasswordException;
import com.infy.repository.UserRepository;
import com.infy.security.CurrentUserResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** Minimum accepted length; SRS: 0-3 chars rejected, 4+ accepted (and classified weak/medium/strong). */
    private static final int MIN_PASSWORD_LENGTH = 4;

    private static final String FORGOT_PASSWORD_SESSION_PREFIX = "FORGOT_PASSWORD_VERIFIED_";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ModelMapper modelMapper;

    @Override
    public LoginResponseDto login(LoginRequestDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        logger.info("Login attempt for username: {}", request.getUsername());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (DisabledException ex) {
            logger.warn("Login rejected - user is deactivated: {}", request.getUsername());
            throw new UserDeactivatedException("User is Deactivated");
        } catch (BadCredentialsException ex) {
            logger.warn("Login rejected - bad credentials for username: {}", request.getUsername());
            throw new InvalidCredentialsException("Wrong username or password");
        }

        // Persist the authenticated context into the HTTP session so the
        // browser's JSESSIONID cookie carries the session on every later request.
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("No user found with username: " + request.getUsername()));

        logger.info("Login succeeded for username: {} with role: {}", user.getUsername(), user.getRole());

        // username/role come from the entity via the shared ModelMapper bean;
        // the welcome message has no entity-side equivalent, so it's set afterwards.
        LoginResponseDto response = modelMapper.map(user, LoginResponseDto.class);
        response.setMessage(welcomeMessageFor(user));
        return response;
    }

    @Override
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Logout requested for username: {}", authentication != null ? authentication.getName() : "unknown");
        new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, authentication);
    }

    @Override
    public void changePassword(ChangePasswordRequestDto request) {
        String username = currentUserResolver.getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("No user found with username: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            logger.warn("Change password rejected - current password incorrect for username: {}", username);
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        validateNewPassword(request.getNewPassword(), request.getConfirmPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        logger.info("Password changed successfully for username: {}", username);
    }

    @Override
    public SecretQuestion getSecretQuestion(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("No user found with username: " + username));
        logger.info("Secret question requested for username: {}", username);
        return user.getSecretQuestion();
    }

    @Override
    public void verifySecretAnswer(ForgotPasswordStep2RequestDto request, HttpServletRequest httpRequest) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("No user found with username: " + request.getUsername()));

        if (!user.getSecretAnswer().equalsIgnoreCase(request.getAnswer().trim())) {
            logger.warn("Forgot-password answer verification failed for username: {}", request.getUsername());
            throw new InvalidSecretAnswerException("Secret answer is incorrect");
        }

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(FORGOT_PASSWORD_SESSION_PREFIX + request.getUsername(), Boolean.TRUE);
        logger.info("Forgot-password answer verified for username: {}", request.getUsername());
    }

    @Override
    public void resetPassword(ResetPasswordRequestDto request, HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        Object verifiedFlag = session != null
                ? session.getAttribute(FORGOT_PASSWORD_SESSION_PREFIX + request.getUsername())
                : null;

        if (!Boolean.TRUE.equals(verifiedFlag)) {
            logger.warn("Reset password rejected - secret answer not verified for username: {}", request.getUsername());
            throw new InvalidSecretAnswerException("Secret answer must be verified before resetting the password");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("No user found with username: " + request.getUsername()));

        validateNewPassword(request.getNewPassword(), request.getConfirmPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        session.removeAttribute(FORGOT_PASSWORD_SESSION_PREFIX + request.getUsername());
        logger.info("Password reset successfully for username: {}", request.getUsername());
    }

    private void validateNewPassword(String newPassword, String confirmPassword) {
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new WeakPasswordException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new PasswordMismatchException("New password and confirm password do not match");
        }
    }

    private String welcomeMessageFor(User user) {
        return "Welcome, " + user.getUsername() + "! You are logged in as " + user.getRole().name() + ".";
    }
}
