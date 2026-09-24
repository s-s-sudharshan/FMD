package com.infy.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.ActivateUserRequestDto;
import com.infy.dto.ApiResponseDto;
import com.infy.dto.ChangePasswordRequestDto;
import com.infy.dto.ChangeRoleRequestDto;
import com.infy.dto.DeactivateUserRequestDto;
import com.infy.dto.PagedResponseDto;
import com.infy.dto.UserRequestDto;
import com.infy.dto.UserResponseDto;
import com.infy.service.AuthService;
import com.infy.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/**
 * Phase 1 built only PUT /change-password here (FE US09), owned by
 * AuthService per the resolution recorded in tasks/todo.md -- left untouched.
 * Phase 2 added Admin User Management (FE US03-US06 / BE US02-US05), backed
 * by UserService/UserServiceImpl, all restricted to ADMIN via @PreAuthorize.
 * PUT /activate (reactivate a soft-deleted user) is the counterpart of
 * PUT /deactivate.
 *
 * @Validated (class-level) + @Min(0) on getAllUsers' page param fixes P2 in
 * phase2_review_actions.md: page=-1 used to reach PageRequest.of(-1, 10),
 * throw IllegalArgumentException, and surface as a 500 via the generic
 * exception handler instead of a 400 validation error.
 *
 * GET /api/users returns a PagedResponseDto (content + totalPages etc.)
 * instead of a bare list, so the frontend can render page numbers.
 */
@RestController
@RequestMapping("/api/users")
@Validated
public class UserAPI {

    private static final Logger logger = LoggerFactory.getLogger(UserAPI.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private Environment environment;

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponseDto<Void>> changePassword(@Valid @RequestBody ChangePasswordRequestDto request) {
        logger.info("Received change-password request");
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .success(true)
                .message(environment.getProperty("API.PASSWORD_CHANGE_SUCCESS", "Password changed successfully"))
                .build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<PagedResponseDto<UserResponseDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{user.page.negative}") int page) {
        logger.info("Received get-all-users request for page {}", page);
        PagedResponseDto<UserResponseDto> users = userService.getAllUsers(page);
        return ResponseEntity.ok(ApiResponseDto.<PagedResponseDto<UserResponseDto>>builder()
                .success(true)
                .message(environment.getProperty("API.USERS_RETRIEVED", "Users retrieved successfully"))
                .data(users)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> addUser(@Valid @RequestBody UserRequestDto request) {
        logger.info("Received add-user request for username: {}", request.getUsername());
        UserResponseDto created = userService.addUser(request);
        return ResponseEntity.ok(ApiResponseDto.<UserResponseDto>builder()
                .success(true)
                .message(environment.getProperty("API.USER_ADDED", "User added successfully"))
                .data(created)
                .build());
    }

    @PutMapping("/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> changeUserRole(@Valid @RequestBody ChangeRoleRequestDto request) {
        logger.info("Received change-role request for username: {}", request.getUsername());
        userService.changeUserRole(request);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .success(true)
                .message(environment.getProperty("API.ROLE_CHANGED", "User role changed successfully"))
                .build());
    }

    @PutMapping("/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> deactivateUser(@Valid @RequestBody DeactivateUserRequestDto request) {
        logger.info("Received deactivate-user request for username: {}", request.getUsername());
        userService.deactivateUser(request);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .success(true)
                .message(environment.getProperty("API.USER_DEACTIVATED", "User deactivated successfully"))
                .build());
    }

    @PutMapping("/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> activateUser(@Valid @RequestBody ActivateUserRequestDto request) {
        logger.info("Received activate-user request for username: {}", request.getUsername());
        userService.activateUser(request);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .success(true)
                .message(environment.getProperty("API.USER_ACTIVATED", "User activated successfully"))
                .build());
    }
}
