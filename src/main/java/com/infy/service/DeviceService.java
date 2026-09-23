package com.infy.service;

import java.util.List;

import com.infy.dto.DeactivateDeviceRequestDto;
import com.infy.dto.DeviceRequestDto;
import com.infy.dto.DeviceResponseDto;
import com.infy.dto.EditDeviceRequestDto;

public interface DeviceService {

    List<DeviceResponseDto> getAllActiveDevices(int page);

    DeviceResponseDto addDevice(DeviceRequestDto request);

    void editDevice(EditDeviceRequestDto request);

    void deactivateDevice(DeactivateDeviceRequestDto request);
}
