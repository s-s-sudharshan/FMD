package com.infy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.infy.entity.Alarm;

/** JpaSpecificationExecutor powers the optional deviceIp/severity/status filters on the list endpoint. */
@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long>, JpaSpecificationExecutor<Alarm> {

    /** Rows of [Severity, Long count]; severities with no alarms are absent (the service fills zeros). */
    @Query("select a.severity, count(a) from Alarm a group by a.severity")
    List<Object[]> countGroupedBySeverity();

    /** Rows of [AlarmStatus, Long count]; statuses with no alarms are absent. */
    @Query("select a.status, count(a) from Alarm a group by a.status")
    List<Object[]> countGroupedByStatus();
}
