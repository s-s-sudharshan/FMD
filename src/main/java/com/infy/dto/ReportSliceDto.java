package com.infy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One pie-chart slice: an enum name and how many alarms fall under it. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSliceDto {
    private String label;
    private long count;
}
