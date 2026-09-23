package com.infy.dto;

import com.infy.enums.ReportType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Bound from the query string of GET /api/reports. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequestDto {

    @NotNull(message = "{report.type.absent}")
    private ReportType type;
}
