package com.infy.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.ApiResponseDto;
import com.infy.dto.DeactivateDeviceRequestDto;
import com.infy.dto.DeviceRequestDto;
import com.infy.dto.DeviceResponseDto;
import com.infy.dto.EditDeviceRequestDto;
import com.infy.service.DeviceService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/** Operator Device Management (FE US11-US14 / BE US07-US10). All endpoints OPERATOR-only. */
@RestController
@RequestMapping("/api/devices")
@Validated
public class DeviceAPI {

    private static final Logger logger = LoggerFactory.getLogger(DeviceAPI.class);

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private Environment environment;

    @GetMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponseDto<List<DeviceResponseDto>>> getAllDevices(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{device.page.negative}") int page) {
        logger.info("Received get-all-devices request for page {}", page);
        List<DeviceResponseDto> devices = deviceService.getAllActiveDevices(page);
        return ResponseEntity.ok(ApiResponseDto.<List<DeviceResponseDto>>builder()
                .success(true)
                .message(environment.getProperty("API.DEVICES_RETRIEVED", "Devices retrieved successfully"))
                .data(devices)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponseDto<DeviceResponseDto>> addDevice(@Valid @RequestBody DeviceRequestDto request) {
        logger.info("Received add-device request for serial: {}", request.getSerialNumber());
        DeviceResponseDto created = deviceService.addDevice(request);
        return ResponseEntity.ok(ApiResponseDto.<DeviceResponseDto>builder()
                .success(true)
                .message(environment.getProperty("API.DEVICE_ADDED", "Device added successfully"))
                .data(created)
                .build());
    }

    @PutMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponseDto<Void>> editDevice(@Valid @RequestBody EditDeviceRequestDto request) {
        logger.info("Received edit-device request for serial: {}", request.getSerialNumber());
        deviceService.editDevice(request);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .success(true)
                .message(environment.getProperty("API.DEVICE_EDITED", "Device updated successfully"))
                .build());
    }

    @PutMapping("/deactivate")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponseDto<Void>> deactivateDevice(@Valid @RequestBody DeactivateDeviceRequestDto request) {
        logger.info("Received deactivate-device request for serial: {}", request.getSerialNumber());
        deviceService.deactivateDevice(request);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .success(true)
                .message(environment.getProperty("API.DEVICE_DEACTIVATED", "Device deactivated successfully"))
                .build());
    }
}
