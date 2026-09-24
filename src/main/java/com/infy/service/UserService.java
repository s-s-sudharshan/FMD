package com.infy.service;

import com.infy.dto.ActivateUserRequestDto;
import com.infy.dto.ChangeRoleRequestDto;
import com.infy.dto.DeactivateUserRequestDto;
import com.infy.dto.PagedResponseDto;
import com.infy.dto.UserRequestDto;
import com.infy.dto.UserResponseDto;

public interface UserService {

    /** @param search optional case-insensitive partial username match; null/blank = no filter. */
    PagedResponseDto<UserResponseDto> getAllUsers(int page, String search);

    UserResponseDto addUser(UserRequestDto request);

    void changeUserRole(ChangeRoleRequestDto request);

    void deactivateUser(DeactivateUserRequestDto request);

    void activateUser(ActivateUserRequestDto request);
}
