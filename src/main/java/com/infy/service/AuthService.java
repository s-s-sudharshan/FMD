package com.infy.service;

import com.infy.dto.ChangePasswordRequestDto;
import com.infy.dto.ForgotPasswordStep2RequestDto;
import com.infy.dto.LoginRequestDto;
import com.infy.dto.LoginResponseDto;
import com.infy.dto.ResetPasswordRequestDto;
import com.infy.enums.SecretQuestion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    LoginResponseDto login(LoginRequestDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    void changePassword(ChangePasswordRequestDto request);

    SecretQuestion getSecretQuestion(String username);

    void verifySecretAnswer(ForgotPasswordStep2RequestDto request, HttpServletRequest httpRequest);

    void resetPassword(ResetPasswordRequestDto request, HttpServletRequest httpRequest);
}
