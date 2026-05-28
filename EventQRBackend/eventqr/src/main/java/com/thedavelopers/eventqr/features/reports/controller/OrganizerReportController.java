package com.thedavelopers.eventqr.features.reports.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedavelopers.eventqr.features.reports.model.dto.EventReportSnapshot;
import com.thedavelopers.eventqr.features.reports.service.ReportService;
import com.thedavelopers.eventqr.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/organizer/events/{eventId}/reports")
public class OrganizerReportController {

    private final ReportService reportService;

    public OrganizerReportController(ReportService reportService) {
        this.reportService = reportService;
    }


    @GetMapping("/attendance")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> attendance(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @GetMapping("/entries")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> entries(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @GetMapping("/exits")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> exits(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> claims(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @GetMapping("/booth-visits")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> boothVisits(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @GetMapping("/rewards")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> rewards(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @GetMapping("/points")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> points(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @PostMapping("/export")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> export(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success("Export prepared", reportService.generate(eventId)));
    }
}