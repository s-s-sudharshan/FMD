package com.infy.api;

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

import com.infy.dto.ActivateDeviceRequestDto;
import com.infy.dto.ApiResponseDto;
import com.infy.dto.DeactivateDeviceRequestDto;
import com.infy.dto.DeviceRequestDto;
import com.infy.dto.DeviceResponseDto;
import com.infy.dto.EditDeviceRequestDto;
import com.infy.dto.PagedResponseDto;
import com.infy.enums.DeviceState;
import com.infy.service.DeviceService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Operator Device Management (FE US11-US14 / BE US07-US10) plus reactivate and search. All endpoints OPERATOR-only. */
@RestController
@RequestMapping("/api/devices")
@Validated
public class DeviceAPI {

    private static final Logger logger = LoggerFactory.getLogger(DeviceAPI.class);

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private Environment environment;

    /**
     * state defaults to ACTIVATED, so existing callers behave exactly as before.
     * search (optional, max 100 chars) is a case-insensitive partial match on serial number or IP.
     */
    @GetMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponseDto<PagedResponseDto<DeviceResponseDto>>> getDevices(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{device.page.negative}") int page,
            @RequestParam(defaultValue = "ACTIVATED") DeviceState state,
            @RequestParam(required = false) @Size(max = 100, message = "{search.tooLong}") String search) {
        logger.info("Received get-devices request for page {}, state {} and search '{}'", page, state, search);
        PagedResponseDto<DeviceResponseDto> devices = deviceService.getDevices(page, state, search);
        return ResponseEntity.ok(ApiResponseDto.<PagedResponseDto<DeviceResponseDto>>builder()
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

    @PutMapping("/activate")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponseDto<Void>> activateDevice(@Valid @RequestBody ActivateDeviceRequestDto request) {
        logger.info("Received activate-device request for serial: {}", request.getSerialNumber());
        deviceService.activateDevice(request);
        return ResponseEntity.ok(ApiResponseDto.<Void>builder()
                .success(true)
                .message(environment.getProperty("API.DEVICE_ACTIVATED", "Device activated successfully"))
                .build());
    }
}
