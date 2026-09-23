package com.infy.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAlarmActionRequestDto {

    @NotEmpty(message = "{alarm.ids.absent}")
    private List<@NotNull(message = "{alarm.ids.absent}") Long> alarmIds;
}
