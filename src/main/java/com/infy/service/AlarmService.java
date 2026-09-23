package com.infy.service;

import java.util.List;

import com.infy.dto.AlarmNoteUpdateRequestDto;
import com.infy.dto.AlarmResponseDto;
import com.infy.dto.AlarmSearchRequestDto;
import com.infy.dto.BulkAlarmActionRequestDto;

public interface AlarmService {

    List<AlarmResponseDto> getAllAlarms(AlarmSearchRequestDto filter, int page);

    void acknowledgeAlarm(Long id);

    void acknowledgeAlarmsBulk(BulkAlarmActionRequestDto request);

    void clearAlarm(Long id);

    void clearAlarmsBulk(BulkAlarmActionRequestDto request);

    void terminateAlarm(Long id);

    void updateNotes(Long id, AlarmNoteUpdateRequestDto request);

    /** Design hook for the deferred Simulator (BE US16, Phase 7). Not implemented yet. */
    void ingestAlarmsFromXml(String xml);
}
