package com.infy.dto;

import com.infy.enums.Role;
import com.infy.enums.UserState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Never exposes password/secretAnswer — plan.md Section 4. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private Long id;
    private String username;
    private Role role;
    private UserState userState;
}
