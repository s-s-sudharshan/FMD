package com.infy.service;

import com.infy.dto.ActivateDeviceRequestDto;
import com.infy.dto.DeactivateDeviceRequestDto;
import com.infy.dto.DeviceRequestDto;
import com.infy.dto.DeviceResponseDto;
import com.infy.dto.EditDeviceRequestDto;
import com.infy.dto.PagedResponseDto;
import com.infy.enums.DeviceState;

public interface DeviceService {

    /** Paged device list for the given state (ACTIVATED or DEACTIVATED). */
    PagedResponseDto<DeviceResponseDto> getDevices(int page, DeviceState state);

    DeviceResponseDto addDevice(DeviceRequestDto request);

    void editDevice(EditDeviceRequestDto request);

    void deactivateDevice(DeactivateDeviceRequestDto request);

    void activateDevice(ActivateDeviceRequestDto request);
}
