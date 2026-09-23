package com.infy.service;

import com.infy.dto.ReportRequestDto;
import com.infy.dto.ReportResponseDto;

public interface ReportService {

    ReportResponseDto generateReport(ReportRequestDto request);
}
