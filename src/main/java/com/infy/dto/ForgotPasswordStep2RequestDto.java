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
public class ForgotPasswordStep2RequestDto {

    @NotBlank(message = "{auth.username.absent}")
    private String username;

    @NotBlank(message = "{auth.answer.absent}")
    private String answer;
}
