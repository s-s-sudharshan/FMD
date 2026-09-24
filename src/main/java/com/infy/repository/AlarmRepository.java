package com.infy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.infy.entity.Alarm;
import com.infy.entity.Device;
import com.infy.enums.AlarmStatus;
import com.infy.enums.Severity;
import com.infy.enums.TrapType;

/** JpaSpecificationExecutor powers the optional deviceIp/severity/status filters on the list endpoint. */
@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long>, JpaSpecificationExecutor<Alarm> {

    /** Rows of [Severity, Long count]; severities with no alarms are absent (the service fills zeros). */
    @Query("select a.severity, count(a) from Alarm a group by a.severity")
    List<Object[]> countGroupedBySeverity();

    /** Rows of [AlarmStatus, Long count]; statuses with no alarms are absent. */
    @Query("select a.status, count(a) from Alarm a group by a.status")
    List<Object[]> countGroupedByStatus();
    
    /** Latest not-yet-terminated alarm of the same kind on a device; used to bump `occurrence` instead of duplicating. */
    Optional<Alarm> findFirstByDeviceAndTrapAndSeverityAndStatusNotOrderByIdDesc(
            Device device, TrapType trap, Severity severity, AlarmStatus status);
}
