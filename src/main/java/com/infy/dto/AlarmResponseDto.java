package com.infy.dto;

import java.time.LocalDateTime;

import com.infy.enums.AlarmStatus;
import com.infy.enums.DeviceType;
import com.infy.enums.Severity;
import com.infy.enums.TrapType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlarmResponseDto {
    private Long id;
    private String deviceIp;
    private String serialNumber;
    private DeviceType deviceType;
    private Severity severity;
    private TrapType trap;
    private String notes;
    private Integer occurrence;
    private AlarmStatus status;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private String clearedBy;
    private LocalDateTime clearedAt;
    private String terminatedBy;
    private LocalDateTime terminatedAt;
}
