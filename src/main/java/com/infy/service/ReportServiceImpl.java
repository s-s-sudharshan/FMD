package com.infy.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.ReportRequestDto;
import com.infy.dto.ReportResponseDto;
import com.infy.dto.ReportSliceDto;
import com.infy.enums.AlarmStatus;
import com.infy.enums.ReportType;
import com.infy.enums.Severity;
import com.infy.repository.AlarmRepository;

/**
 * BE US06 (Phase 5 Report). Counts ALL alarms (any status, any device state)
 * so history stays reportable. Every enum value gets a slice, including
 * zero-count ones, so the chart's categories are stable.
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);

    @Autowired
    private AlarmRepository alarmRepository;

    @Override
    @Transactional(readOnly = true)
    public ReportResponseDto generateReport(ReportRequestDto request) {
        ReportType type = request.getType();
        List<ReportSliceDto> slices = switch (type) {
            case SEVERITY -> buildSlices(Severity.values(), alarmRepository.countGroupedBySeverity());
            case STATUS -> buildSlices(AlarmStatus.values(), alarmRepository.countGroupedByStatus());
        };
        logger.info("Generated {} report with {} slice(s)", type, slices.size());
        return ReportResponseDto.builder().type(type).slices(slices).build();
    }

    private List<ReportSliceDto> buildSlices(Enum<?>[] allValues, List<Object[]> rows) {
        Map<Object, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put(row[0], ((Number) row[1]).longValue());
        }
        return Stream.of(allValues)
                .map(value -> new ReportSliceDto(value.name(), counts.getOrDefault(value, 0L)))
                .collect(Collectors.toList());
    }
}
