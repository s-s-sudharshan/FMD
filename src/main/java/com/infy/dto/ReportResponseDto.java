package com.infy.dto;

import java.util.List;

import com.infy.enums.ReportType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseDto {
    private ReportType type;
    private List<ReportSliceDto> slices;
}
