package com.infy.dto;

import com.infy.enums.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeRoleRequestDto {

    @NotBlank(message = "{user.username.absent}")
    private String username;

    @NotNull(message = "{user.newRole.absent}")
    private Role newRole;
}
