package com.infy.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.infy.dto.AlarmNoteUpdateRequestDto;
import com.infy.dto.AlarmResponseDto;
import com.infy.dto.AlarmSearchRequestDto;
import com.infy.dto.BulkAlarmActionRequestDto;
import com.infy.dto.PagedResponseDto;
import com.infy.entity.Alarm;
import com.infy.entity.Device;
import com.infy.enums.AlarmStatus;
import com.infy.enums.DeviceState;
import com.infy.exception.AlarmNotFoundException;
import com.infy.exception.InvalidAlarmStateTransitionException;
import com.infy.repository.AlarmRepository;
import com.infy.repository.DeviceRepository;
import com.infy.security.CurrentUserResolver;
import com.infy.simulator.AlarmXmlParser;

import jakarta.persistence.criteria.Predicate;

/**
 * BE US12-US15 (Phase 4 Manager Fault Handling) plus audit trail (Extra 3).
 *
 * Entity->DTO mapping is done by hand here (not ModelMapper): Alarm carries
 * both denormalized fields (deviceIp, serialNumber, deviceType) and a
 * {@code device} relation with same-named properties, which makes
 * ModelMapper's implicit matching ambiguous and fails at startup.
 *
 * Bulk operations are all-or-nothing: every id is validated before any
 * status is changed, inside one transaction. A bulk action stamps every alarm
 * with the same user and timestamp. The actor always comes from the security
 * context (CurrentUserResolver), never from a request body.
 */
@Service
public class AlarmServiceImpl implements AlarmService {

    private static final Logger logger = LoggerFactory.getLogger(AlarmServiceImpl.class);

    private static final int PAGE_SIZE = 10;

    private static final Set<AlarmStatus> ACK_FROM = EnumSet.of(AlarmStatus.UNACKNOWLEDGED);
    private static final Set<AlarmStatus> CLEAR_FROM = EnumSet.of(AlarmStatus.ACKNOWLEDGED);
    private static final Set<AlarmStatus> TERMINATE_FROM = EnumSet.of(AlarmStatus.ACKNOWLEDGED, AlarmStatus.CLEARED);

    /** Statuses a duplicate simulator event may correlate to. CLEARED/TERMINATED alarms are never modified by ingestion. */
    private static final Set<AlarmStatus> ACTIVE = EnumSet.of(AlarmStatus.UNACKNOWLEDGED, AlarmStatus.ACKNOWLEDGED);

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private AlarmXmlParser alarmXmlParser;

    @Autowired
    private CurrentUserResolver currentUserResolver;

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AlarmResponseDto> getAllAlarms(AlarmSearchRequestDto filter, int page) {
        AlarmSearchRequestDto f = filter != null ? filter : new AlarmSearchRequestDto();
        Page<Alarm> result = alarmRepository.findAll(buildSpecification(f),
                PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        logger.info("Fetched alarm list page {} ({} of {} total matching alarms, {} page(s))",
                page, result.getNumberOfElements(), result.getTotalElements(), result.getTotalPages());
        List<AlarmResponseDto> content = result.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return PagedResponseDto.from(result, content);
    }

    @Override
    @Transactional
    public void acknowledgeAlarm(Long id) {
        transition(id, ACK_FROM, AlarmStatus.ACKNOWLEDGED, "Service.ALARM_NOT_UNACKNOWLEDGED");
    }

    @Override
    @Transactional
    public void acknowledgeAlarmsBulk(BulkAlarmActionRequestDto request) {
        transitionBulk(request.getAlarmIds(), ACK_FROM, AlarmStatus.ACKNOWLEDGED, "Service.ALARM_NOT_UNACKNOWLEDGED");
    }

    @Override
    @Transactional
    public void clearAlarm(Long id) {
        transition(id, CLEAR_FROM, AlarmStatus.CLEARED, "Service.ALARM_NOT_ACKNOWLEDGED");
    }

    @Override
    @Transactional
    public void clearAlarmsBulk(BulkAlarmActionRequestDto request) {
        transitionBulk(request.getAlarmIds(), CLEAR_FROM, AlarmStatus.CLEARED, "Service.ALARM_NOT_ACKNOWLEDGED");
    }

    @Override
    @Transactional
    public void terminateAlarm(Long id) {
        transition(id, TERMINATE_FROM, AlarmStatus.TERMINATED, "Service.ALARM_CANNOT_TERMINATE");
    }

    @Override
    @Transactional
    public void updateNotes(Long id, AlarmNoteUpdateRequestDto request) {
        Alarm alarm = findAlarm(id);
        alarm.setNotes(request.getNotes());
        alarmRepository.save(alarm);
        logger.info("Notes updated for alarm {}", id);
    }

    /**
     * Correlation rule: a duplicate event (same device + trap + severity) only matches an ACTIVE
     * alarm (UNACKNOWLEDGED/ACKNOWLEDGED). It increments `occurrence` and changes nothing else --
     * not the status, not the audit fields, not the notes. If the latest match is CLEARED or
     * TERMINATED (or there is none), a new UNACKNOWLEDGED alarm is created with occurrence 1 and
     * null audit fields, and the completed row stays untouched.
     */
    @Override
    @Transactional
    public int ingestAlarmsFromXml(String xml) {
        int ingested = 0;
        for (AlarmXmlParser.ParsedAlarm p : alarmXmlParser.parse(xml)) {
            Optional<Device> found = deviceRepository.findByIpAddress(p.deviceIp());
            if (found.isEmpty() || found.get().getDeviceState() != DeviceState.ACTIVATED) {
                logger.warn("Ingest skipped - no active device with IP {}", p.deviceIp());
                continue;
            }
            Device device = found.get();
            Optional<Alarm> existing = alarmRepository
                    .findFirstByDeviceAndTrapAndSeverityAndStatusInOrderByIdDesc(
                            device, p.trap(), p.severity(), ACTIVE);
            if (existing.isPresent()) {
                Alarm alarm = existing.get();
                alarm.setOccurrence(alarm.getOccurrence() + 1);
                alarmRepository.save(alarm);
            } else {
                alarmRepository.save(Alarm.builder()
                        .device(device)
                        .deviceIp(device.getIpAddress())
                        .serialNumber(device.getSerialNumber())
                        .deviceType(device.getDeviceType())
                        .severity(p.severity())
                        .trap(p.trap())
                        .notes(p.notes())
                        .build());
            }
            ingested++;
        }
        logger.info("Ingested {} alarm(s) from XML", ingested);
        return ingested;
    }

    /**
     * Default view = alarms of ACTIVATED devices, excluding TERMINATED alarms
     * (they drop out of the fault-handling table, per plan.md Section 9).
     * Passing status=TERMINATED explicitly still returns them.
     */
    private Specification<Alarm> buildSpecification(AlarmSearchRequestDto f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("device").get("deviceState"), DeviceState.ACTIVATED));
            if (StringUtils.hasText(f.getDeviceIp())) {
                predicates.add(cb.like(root.get("deviceIp"), "%" + f.getDeviceIp().trim() + "%"));
            }
            if (f.getSeverity() != null) {
                predicates.add(cb.equal(root.get("severity"), f.getSeverity()));
            }
            if (f.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), f.getStatus()));
            } else {
                predicates.add(cb.notEqual(root.get("status"), AlarmStatus.TERMINATED));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void transition(Long id, Set<AlarmStatus> allowedFrom, AlarmStatus target, String errorKey) {
        // Resolved first so an unauthenticated call fails before anything is read or changed.
        String actor = currentUserResolver.getCurrentUsername();
        Alarm alarm = findAlarm(id);
        if (!allowedFrom.contains(alarm.getStatus())) {
            logger.warn("Alarm {} cannot move {} -> {}", id, alarm.getStatus(), target);
            throw new InvalidAlarmStateTransitionException(errorKey);
        }
        stamp(alarm, target, actor, LocalDateTime.now());
        alarmRepository.save(alarm);
        logger.info("Alarm {} moved to {} by {}", id, target, actor);
    }

    private void transitionBulk(List<Long> ids, Set<AlarmStatus> allowedFrom, AlarmStatus target, String errorKey) {
        String actor = currentUserResolver.getCurrentUsername();
        Set<Long> uniqueIds = new LinkedHashSet<>(ids);
        List<Alarm> alarms = alarmRepository.findAllById(uniqueIds);
        if (alarms.size() != uniqueIds.size()) {
            logger.warn("Bulk {} rejected - one or more alarm ids not found: {}", target, uniqueIds);
            throw new AlarmNotFoundException("Service.ALARM_NOT_FOUND");
        }
        List<Long> invalid = alarms.stream()
                .filter(a -> !allowedFrom.contains(a.getStatus()))
                .map(Alarm::getId)
                .collect(Collectors.toList());
        if (!invalid.isEmpty()) {
            logger.warn("Bulk {} rejected - alarms in an invalid state: {}", target, invalid);
            throw new InvalidAlarmStateTransitionException(errorKey);
        }
        LocalDateTime now = LocalDateTime.now(); // one user + one timestamp for the whole batch
        alarms.forEach(a -> stamp(a, target, actor, now));
        alarmRepository.saveAll(alarms);
        logger.info("{} alarm(s) moved to {} by {}: {}", alarms.size(), target, actor, uniqueIds);
    }

    /** Sets the new status and only that transition's audit pair; earlier pairs stay intact. */
    private void stamp(Alarm alarm, AlarmStatus target, String actor, LocalDateTime at) {
        alarm.setStatus(target);
        switch (target) {
            case ACKNOWLEDGED -> {
                alarm.setAcknowledgedBy(actor);
                alarm.setAcknowledgedAt(at);
            }
            case CLEARED -> {
                alarm.setClearedBy(actor);
                alarm.setClearedAt(at);
            }
            case TERMINATED -> {
                alarm.setTerminatedBy(actor);
                alarm.setTerminatedAt(at);
            }
            default -> throw new IllegalArgumentException("No audit stamp for status " + target);
        }
    }

    private Alarm findAlarm(Long id) {
        return alarmRepository.findById(id)
                .orElseThrow(() -> new AlarmNotFoundException("Service.ALARM_NOT_FOUND"));
    }

    private AlarmResponseDto toDto(Alarm a) {
        return AlarmResponseDto.builder()
                .id(a.getId())
                .deviceIp(a.getDeviceIp())
                .serialNumber(a.getSerialNumber())
                .deviceType(a.getDeviceType())
                .severity(a.getSeverity())
                .trap(a.getTrap())
                .notes(a.getNotes())
                .occurrence(a.getOccurrence())
                .status(a.getStatus())
                .acknowledgedBy(a.getAcknowledgedBy())
                .acknowledgedAt(a.getAcknowledgedAt())
                .clearedBy(a.getClearedBy())
                .clearedAt(a.getClearedAt())
                .terminatedBy(a.getTerminatedBy())
                .terminatedAt(a.getTerminatedAt())
                .build();
    }
}
