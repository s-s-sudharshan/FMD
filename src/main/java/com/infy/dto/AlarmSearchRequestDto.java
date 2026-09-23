package com.infy.dto;

import com.infy.enums.AlarmStatus;
import com.infy.enums.Severity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Optional query-string filters for GET /api/alarms. All fields may be null. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlarmSearchRequestDto {
    private String deviceIp;
    private Severity severity;
    private AlarmStatus status;
}
