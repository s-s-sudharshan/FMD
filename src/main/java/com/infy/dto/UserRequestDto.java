package com.infy.dto;

import com.infy.enums.Role;
import com.infy.enums.SecretQuestion;

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
public class UserRequestDto {

    @NotBlank(message = "{user.username.absent}")
    private String username;

    @NotBlank(message = "{user.password.absent}")
    private String password;

    @NotNull(message = "{user.role.absent}")
    private Role role;

    @NotNull(message = "{user.secretQuestion.absent}")
    private SecretQuestion secretQuestion;

    @NotBlank(message = "{user.secretAnswer.absent}")
    private String secretAnswer;
}
