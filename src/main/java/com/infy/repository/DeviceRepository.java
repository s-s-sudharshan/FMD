package com.infy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Devices in the given state whose serial number OR IP address contains the
     * search text, case-insensitively (extra feature: search). A '%' or '_'
     * typed by the user acts as a SQL wildcard -- accepted per the plan.
     */
    @Query("select d from Device d where d.deviceState = :state "
            + "and (lower(d.serialNumber) like lower(concat('%', :search, '%')) "
            + "or lower(d.ipAddress) like lower(concat('%', :search, '%')))")
    Page<Device> searchByState(@Param("state") DeviceState state, @Param("search") String search, Pageable pageable);

    Optional<Device> findByIpAddress(String ipAddress);

    List<Device> findAllByDeviceState(DeviceState deviceState);
}
