package com.infy.service;

import java.util.List;

import com.infy.dto.ChangeRoleRequestDto;
import com.infy.dto.DeactivateUserRequestDto;
import com.infy.dto.UserRequestDto;
import com.infy.dto.UserResponseDto;

public interface UserService {

    List<UserResponseDto> getAllUsers(int page);

    UserResponseDto addUser(UserRequestDto request);

    void changeUserRole(ChangeRoleRequestDto request);

    void deactivateUser(DeactivateUserRequestDto request);
}
