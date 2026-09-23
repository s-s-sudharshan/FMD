package com.infy.service;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.infy.dto.ChangeRoleRequestDto;
import com.infy.dto.DeactivateUserRequestDto;
import com.infy.dto.UserRequestDto;
import com.infy.dto.UserResponseDto;
import com.infy.entity.User;
import com.infy.enums.UserState;
import com.infy.exception.DuplicateUsernameException;
import com.infy.exception.InvalidRoleChangeException;
import com.infy.exception.UserNotFoundException;
import com.infy.exception.WeakPasswordException;
import com.infy.repository.UserRepository;

/**
 * BE US02-US05 (Phase 2 Admin User Management). The password-strength check
 * below intentionally mirrors AuthServiceImpl.validateNewPassword() (same
 * MIN_PASSWORD_LENGTH rule, same Service.WEAK_PASSWORD message key) rather
 * than being extracted into a shared helper -- AuthServiceImpl is
 * Phase-1-verified code and is left untouched per the "don't modify Phase 1
 * unnecessarily" instruction.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int PAGE_SIZE = 10;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private Environment environment;

    @Override
    public List<UserResponseDto> getAllUsers(int page) {
        Page<User> userPage = userRepository.findAll(PageRequest.of(page, PAGE_SIZE));
        logger.info("Fetched user list page {} ({} of {} total users)",
                page, userPage.getNumberOfElements(), userPage.getTotalElements());
        return userPage.getContent().stream()
                .map(user -> modelMapper.map(user, UserResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDto addUser(UserRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            logger.warn("Add user rejected - username already exists: {}", request.getUsername());
            throw new DuplicateUsernameException("Service.DUPLICATE_USERNAME");
        }
        validatePasswordStrength(request.getPassword());

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .userState(UserState.ACTIVATED)
                .secretQuestion(request.getSecretQuestion())
                .secretAnswer(request.getSecretAnswer())
                .build();

        User saved = userRepository.save(user);
        logger.info("User added: {} with role {}", saved.getUsername(), saved.getRole());
        return modelMapper.map(saved, UserResponseDto.class);
    }

    @Override
    public void changeUserRole(ChangeRoleRequestDto request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Service.USER_NOT_FOUND"));

        if (user.getRole() == request.getNewRole()) {
            logger.warn("Change role rejected - username {} already has role {}",
                    request.getUsername(), request.getNewRole());
            throw new InvalidRoleChangeException("Service.SAME_ROLE");
        }

        user.setRole(request.getNewRole());
        userRepository.save(user);
        logger.info("Role changed for username {} to {}", request.getUsername(), request.getNewRole());
    }

    @Override
    public void deactivateUser(DeactivateUserRequestDto request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Service.USER_NOT_FOUND"));

        user.setUserState(UserState.DEACTIVATED);
        userRepository.save(user);
        logger.info("User deactivated: {}", request.getUsername());
    }

    private void validatePasswordStrength(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            String template = environment.getProperty("Service.WEAK_PASSWORD",
                    "Password must be at least {0} characters long.");
            throw new WeakPasswordException(MessageFormat.format(template, MIN_PASSWORD_LENGTH));
        }
    }
}
