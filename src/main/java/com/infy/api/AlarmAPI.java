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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.AlarmNoteUpdateRequestDto;
import com.infy.dto.AlarmResponseDto;
import com.infy.dto.AlarmSearchRequestDto;
import com.infy.dto.ApiResponseDto;
import com.infy.dto.BulkAlarmActionRequestDto;
import com.infy.service.AlarmService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/**
 * Manager Fault Handling (FE US16-US19 / BE US12-US15). Read is open to any
 * authenticated user; every action is MANAGER-only.
 */
@RestController
@RequestMapping("/api/alarms")
@Validated
public class AlarmAPI {

    private static final Logger logger = LoggerFactory.getLogger(AlarmAPI.class);

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private Environment environment;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<AlarmResponseDto>>> getAllAlarms(
            AlarmSearchRequestDto filter,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{alarm.page.negative}") int page) {
        logger.info("Received get-alarms request for page {} with filter {}", page, filter);
        List<AlarmResponseDto> alarms = alarmService.getAllAlarms(filter, page);
        return ok("API.ALARMS_RETRIEVED", "Alarms retrieved successfully", alarms);
    }

    @PutMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponseDto<Void>> acknowledge(@PathVariable Long id) {
        logger.info("Received acknowledge request for alarm {}", id);
        alarmService.acknowledgeAlarm(id);
        return ok("API.ALARM_ACKNOWLEDGED", "Alarm acknowledged successfully", null);
    }

    @PutMapping("/acknowledge/bulk")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponseDto<Void>> acknowledgeBulk(@Valid @RequestBody BulkAlarmActionRequestDto request) {
        logger.info("Received bulk acknowledge request for {} alarm(s)", request.getAlarmIds().size());
        alarmService.acknowledgeAlarmsBulk(request);
        return ok("API.ALARMS_ACKNOWLEDGED", "Alarms acknowledged successfully", null);
    }

    @PutMapping("/{id}/clear")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponseDto<Void>> clear(@PathVariable Long id) {
        logger.info("Received clear request for alarm {}", id);
        alarmService.clearAlarm(id);
        return ok("API.ALARM_CLEARED", "Alarm cleared successfully", null);
    }

    @PutMapping("/clear/bulk")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponseDto<Void>> clearBulk(@Valid @RequestBody BulkAlarmActionRequestDto request) {
        logger.info("Received bulk clear request for {} alarm(s)", request.getAlarmIds().size());
        alarmService.clearAlarmsBulk(request);
        return ok("API.ALARMS_CLEARED", "Alarms cleared successfully", null);
    }

    @PutMapping("/{id}/terminate")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponseDto<Void>> terminate(@PathVariable Long id) {
        logger.info("Received terminate request for alarm {}", id);
        alarmService.terminateAlarm(id);
        return ok("API.ALARM_TERMINATED", "Alarm terminated successfully", null);
    }

    @PutMapping("/{id}/notes")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponseDto<Void>> updateNotes(@PathVariable Long id,
                                                            @Valid @RequestBody AlarmNoteUpdateRequestDto request) {
        logger.info("Received update-notes request for alarm {}", id);
        alarmService.updateNotes(id, request);
        return ok("API.ALARM_NOTES_UPDATED", "Alarm notes updated successfully", null);
    }

    private <T> ResponseEntity<ApiResponseDto<T>> ok(String messageKey, String defaultMessage, T data) {
        return ResponseEntity.ok(ApiResponseDto.<T>builder()
                .success(true)
                .message(environment.getProperty(messageKey, defaultMessage))
                .data(data)
                .build());
    }
}
