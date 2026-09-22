package com.infy.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.ApiResponseDto;
import com.infy.dto.ChangePasswordRequestDto;
import com.infy.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Only the Change Password endpoint (FE US09) lives here for now. plan.md
 * Section 5 assigns changePassword() to AuthService (not a separate
 * UserService) and Section 6 places it at PUT /api/users/change-password
 * (not under /api/auth) -- those two statements only reconcile if this
 * controller exists ahead of Phase 2, which is when the rest of User
 * Management (list/add/change-role/deactivate, backed by a real
 * UserService) is built out per plan.md's own phase breakdown. Rather than
 * pre-build that service now, this class stays deliberately thin and will
 * gain its remaining endpoints in Phase 2.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAPI {

    private static final Logger logger = LoggerFactory.getLogger(UserAPI.class);

    private final AuthService authService;

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponseDto<Void>> changePassword(@Valid @RequestBody ChangePasswordRequestDto request) {
        logger.info("Received change-password request");
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .success(true)
                .message("Password changed successfully")
                .build());
    }
}
