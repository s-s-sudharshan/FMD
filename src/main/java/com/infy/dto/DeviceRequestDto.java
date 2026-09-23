package com.infy.dto;

import com.infy.enums.DeviceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** IPv4 format is checked in the service layer so InvalidIpAddressException can carry a clean message. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceRequestDto {

    @NotBlank(message = "{device.serialNumber.absent}")
    private String serialNumber;

    @NotBlank(message = "{device.ipAddress.absent}")
    private String ipAddress;

    @NotNull(message = "{device.deviceType.absent}")
    private DeviceType deviceType;
}
