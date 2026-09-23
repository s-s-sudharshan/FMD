package com.infy.service;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
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

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** Minimum accepted length; SRS: 0-3 chars rejected, 4+ accepted (and classified weak/medium/strong). */
    private static final int MIN_PASSWORD_LENGTH = 4;

    private static final String FORGOT_PASSWORD_SESSION_PREFIX = "FORGOT_PASSWORD_VERIFIED_AT_";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SecurityContextRepository securityContextRepository;

    @Autowired
    private CurrentUserResolver currentUserResolver;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private Environment environment;

    /**
     * How long a verified secret answer stays usable before a reset must be
     * re-verified (codex review finding #1 -- the original implementation had
     * no expiry at all). Configurable; defaults to 5 minutes.
     */
    @Value("${app.forgot-password.verification-ttl-minutes:5}")
    private long forgotPasswordVerificationTtlMinutes;

    @Override
    public LoginResponseDto login(LoginRequestDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        logger.info("Login attempt for username: {}", request.getUsername());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (DisabledException ex) {
            logger.warn("Login rejected - user is deactivated: {}", request.getUsername());
            throw new UserDeactivatedException("Service.USER_DEACTIVATED");
        } catch (BadCredentialsException ex) {
            logger.warn("Login rejected - bad credentials for username: {}", request.getUsername());
            throw new InvalidCredentialsException("Service.INVALID_CREDENTIALS");
        }

        // Persist the authenticated context into the HTTP session so the
        // browser's JSESSIONID cookie carries the session on every later request.
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Service.USER_NOT_FOUND"));

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
                .orElseThrow(() -> new UserNotFoundException("Service.USER_NOT_FOUND"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            logger.warn("Change password rejected - current password incorrect for username: {}", username);
            throw new InvalidCredentialsException("Service.CURRENT_PASSWORD_INCORRECT");
        }

        validateNewPassword(request.getNewPassword(), request.getConfirmPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        logger.info("Password changed successfully for username: {}", username);
    }

    @Override
    public SecretQuestion getSecretQuestion(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Service.USER_NOT_FOUND"));
        logger.info("Secret question requested for username: {}", username);
        return user.getSecretQuestion();
    }

    @Override
    public void verifySecretAnswer(ForgotPasswordStep2RequestDto request, HttpServletRequest httpRequest) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Service.USER_NOT_FOUND"));

        if (!user.getSecretAnswer().equalsIgnoreCase(request.getAnswer().trim())) {
            logger.warn("Forgot-password answer verification failed for username: {}", request.getUsername());
            throw new InvalidSecretAnswerException("Service.INVALID_SECRET_ANSWER");
        }

        // Store the verification instant (not just a boolean) so resetPassword
        // can enforce a short lifetime instead of trusting the flag for as
        // long as the underlying HTTP session happens to stay alive.
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(FORGOT_PASSWORD_SESSION_PREFIX + request.getUsername(), Instant.now());
        logger.info("Forgot-password answer verified for username: {}, valid for {} minute(s)",
                request.getUsername(), forgotPasswordVerificationTtlMinutes);
    }

    @Override
    public void resetPassword(ResetPasswordRequestDto request, HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        Object verifiedAtAttribute = session != null
                ? session.getAttribute(FORGOT_PASSWORD_SESSION_PREFIX + request.getUsername())
                : null;

        if (!(verifiedAtAttribute instanceof Instant verifiedAt)) {
            logger.warn("Reset password rejected - secret answer not verified for username: {}", request.getUsername());
            throw new InvalidSecretAnswerException("Service.FORGOT_PASSWORD_NOT_VERIFIED");
        }

        Duration ttl = Duration.ofMinutes(forgotPasswordVerificationTtlMinutes);
        if (Duration.between(verifiedAt, Instant.now()).compareTo(ttl) > 0) {
            // Expired: consume it so a stale flag can never be reused, then reject.
            session.removeAttribute(FORGOT_PASSWORD_SESSION_PREFIX + request.getUsername());
            logger.warn("Reset password rejected - secret-answer verification expired for username: {}", request.getUsername());
            throw new InvalidSecretAnswerException("Service.FORGOT_PASSWORD_VERIFICATION_EXPIRED");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Service.USER_NOT_FOUND"));

        validateNewPassword(request.getNewPassword(), request.getConfirmPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        session.removeAttribute(FORGOT_PASSWORD_SESSION_PREFIX + request.getUsername());
        logger.info("Password reset successfully for username: {}", request.getUsername());
    }

    private void validateNewPassword(String newPassword, String confirmPassword) {
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            String template = environment.getProperty("Service.WEAK_PASSWORD",
                    "Password must be at least {0} characters long.");
            throw new WeakPasswordException(MessageFormat.format(template, MIN_PASSWORD_LENGTH));
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new PasswordMismatchException("Service.PASSWORD_MISMATCH");
        }
    }

    private String welcomeMessageFor(User user) {
        return "Welcome, " + user.getUsername() + "! You are logged in as " + user.getRole().name() + ".";
    }
}
