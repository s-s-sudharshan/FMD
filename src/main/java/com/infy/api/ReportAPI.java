package com.infy.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.ApiResponseDto;
import com.infy.dto.ReportRequestDto;
import com.infy.dto.ReportResponseDto;
import com.infy.service.ReportService;

import jakarta.validation.Valid;

/** Report (FE US07 / BE US06). Open to any authenticated role. */
@RestController
@RequestMapping("/api/reports")
public class ReportAPI {

    private static final Logger logger = LoggerFactory.getLogger(ReportAPI.class);

    @Autowired
    private ReportService reportService;

    @Autowired
    private Environment environment;

    @GetMapping
    public ResponseEntity<ApiResponseDto<ReportResponseDto>> getReport(@Valid ReportRequestDto request) {
        logger.info("Received report request of type {}", request.getType());
        ReportResponseDto report = reportService.generateReport(request);
        return ResponseEntity.ok(ApiResponseDto.<ReportResponseDto>builder()
                .success(true)
                .message(environment.getProperty("API.REPORT_GENERATED", "Report generated successfully"))
                .data(report)
                .build());
    }
}
