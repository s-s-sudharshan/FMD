package com.infy.dto;

import com.infy.enums.DeviceState;
import com.infy.enums.DeviceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponseDto {
    private Long id;
    private String serialNumber;
    private String ipAddress;
    private DeviceType deviceType;
    private DeviceState deviceState;
}
