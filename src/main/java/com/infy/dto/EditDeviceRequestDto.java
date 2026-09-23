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
public class EditDeviceRequestDto {

    @NotBlank(message = "{device.serialNumber.absent}")
    private String serialNumber;

    @NotBlank(message = "{device.newIpAddress.absent}")
    private String newIpAddress;
}
