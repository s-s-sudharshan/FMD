package com.infy.service;

import com.infy.dto.AlarmNoteUpdateRequestDto;
import com.infy.dto.AlarmResponseDto;
import com.infy.dto.AlarmSearchRequestDto;
import com.infy.dto.BulkAlarmActionRequestDto;
import com.infy.dto.PagedResponseDto;

public interface AlarmService {

    PagedResponseDto<AlarmResponseDto> getAllAlarms(AlarmSearchRequestDto filter, int page);

    void acknowledgeAlarm(Long id);

    void acknowledgeAlarmsBulk(BulkAlarmActionRequestDto request);

    void clearAlarm(Long id);

    void clearAlarmsBulk(BulkAlarmActionRequestDto request);

    void terminateAlarm(Long id);

    void updateNotes(Long id, AlarmNoteUpdateRequestDto request);

    /** Ingests alarms from simulator XML (BE US16). @return number of alarms ingested (created or re-occurred). */
    int ingestAlarmsFromXml(String xml);
}
