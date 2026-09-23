package com.infy.service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.infy.dto.DeactivateDeviceRequestDto;
import com.infy.dto.DeviceRequestDto;
import com.infy.dto.DeviceResponseDto;
import com.infy.dto.EditDeviceRequestDto;
import com.infy.entity.Device;
import com.infy.enums.DeviceState;
import com.infy.exception.DeviceNotFoundException;
import com.infy.exception.DuplicateDeviceException;
import com.infy.exception.InvalidIpAddressException;
import com.infy.repository.DeviceRepository;

/**
 * BE US07-US10 (Phase 3 Operator Device Management). Devices are soft-deleted
 * only; a deactivated device keeps its serial/IP, so those stay reserved
 * (unique constraints) and alarm history remains valid.
 */
@Service
public class DeviceServiceImpl implements DeviceService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceServiceImpl.class);

    private static final int PAGE_SIZE = 10;

    /** Four dot-separated octets, each 0-255, no leading zeros (except a lone 0). */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<DeviceResponseDto> getAllActiveDevices(int page) {
        Page<Device> devicePage = deviceRepository.findByDeviceState(
                DeviceState.ACTIVATED, PageRequest.of(page, PAGE_SIZE, Sort.by("id")));
        logger.info("Fetched active device list page {} ({} of {} total active devices)",
                page, devicePage.getNumberOfElements(), devicePage.getTotalElements());
        return devicePage.getContent().stream()
                .map(device -> modelMapper.map(device, DeviceResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public DeviceResponseDto addDevice(DeviceRequestDto request) {
        String serialNumber = request.getSerialNumber().trim();
        String ipAddress = request.getIpAddress().trim();

        validateIpAddress(ipAddress);

        if (deviceRepository.existsBySerialNumber(serialNumber)) {
            logger.warn("Add device rejected - serial number already exists: {}", serialNumber);
            throw new DuplicateDeviceException("Service.DUPLICATE_DEVICE_SERIAL");
        }
        if (deviceRepository.existsByIpAddress(ipAddress)) {
            logger.warn("Add device rejected - IP address already exists: {}", ipAddress);
            throw new DuplicateDeviceException("Service.DUPLICATE_DEVICE_IP");
        }

        Device device = Device.builder()
                .serialNumber(serialNumber)
                .ipAddress(ipAddress)
                .deviceType(request.getDeviceType())
                .deviceState(DeviceState.ACTIVATED)
                .build();

        Device saved = deviceRepository.save(device);
        logger.info("Device added: serial {} at {} ({})", saved.getSerialNumber(), saved.getIpAddress(), saved.getDeviceType());
        return modelMapper.map(saved, DeviceResponseDto.class);
    }

    @Override
    public void editDevice(EditDeviceRequestDto request) {
        Device device = findActiveDevice(request.getSerialNumber().trim());
        String newIp = request.getNewIpAddress().trim();

        validateIpAddress(newIp);

        // Unchanged IP is a valid no-op: nothing to persist, and it must not
        // collide with the device's own row in the duplicate check below.
        if (newIp.equals(device.getIpAddress())) {
            logger.info("Edit device: IP unchanged for {}, nothing to update", device.getSerialNumber());
            return;
        }

        // Only a *different* device holding this IP is a conflict.
        if (deviceRepository.existsByIpAddressAndIdNot(newIp, device.getId())) {
            logger.warn("Edit device rejected - IP address belongs to another device: {}", newIp);
            throw new DuplicateDeviceException("Service.DUPLICATE_DEVICE_IP");
        }

        device.setIpAddress(newIp);
        deviceRepository.save(device);
        logger.info("Device {} IP address changed to {}", device.getSerialNumber(), newIp);
    }

    @Override
    public void deactivateDevice(DeactivateDeviceRequestDto request) {
        Device device = findActiveDevice(request.getSerialNumber().trim());
        device.setDeviceState(DeviceState.DEACTIVATED);
        deviceRepository.save(device);
        logger.info("Device deactivated: {}", device.getSerialNumber());
    }

    /** A deactivated device is treated as "not found" for edit/deactivate: it is no longer operator-visible. */
    private Device findActiveDevice(String serialNumber) {
        Device device = deviceRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new DeviceNotFoundException("Service.DEVICE_NOT_FOUND"));
        if (device.getDeviceState() == DeviceState.DEACTIVATED) {
            logger.warn("Operation rejected - device already deactivated: {}", serialNumber);
            throw new DeviceNotFoundException("Service.DEVICE_NOT_FOUND");
        }
        return device;
    }

    private void validateIpAddress(String ipAddress) {
        if (!IPV4_PATTERN.matcher(ipAddress).matches()
                || "0.0.0.0".equals(ipAddress)
                || "255.255.255.255".equals(ipAddress)) {
            logger.warn("Invalid IPv4 address rejected: {}", ipAddress);
            throw new InvalidIpAddressException("Service.INVALID_IP_ADDRESS");
        }
    }
}
