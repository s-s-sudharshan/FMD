package com.infy.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.infy.entity.Device;
import com.infy.enums.DeviceState;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findBySerialNumber(String serialNumber);

    boolean existsBySerialNumber(String serialNumber);

    boolean existsByIpAddress(String ipAddress);

    /** True if any device OTHER than the one with the given id already uses this IP (used by edit). */
    boolean existsByIpAddressAndIdNot(String ipAddress, Long id);

    Page<Device> findByDeviceState(DeviceState deviceState, Pageable pageable);
}
