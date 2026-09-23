package com.infy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequestDto {

    @NotBlank(message = "{auth.currentPassword.absent}")
    private String currentPassword;

    @NotBlank(message = "{auth.newPassword.absent}")
    private String newPassword;

    @NotBlank(message = "{auth.confirmPassword.absent}")
    private String confirmPassword;
}
