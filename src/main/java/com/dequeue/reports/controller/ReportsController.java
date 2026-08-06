package com.dequeue.reports.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.reports.dto.*;
import com.dequeue.reports.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReportsController {
    private final ReportService reportService;

    @GetMapping("/today")
    public ApiResponse<TodayReport> getTodayReport() {
        return ApiResponse.success(reportService.getTodayReport(SecurityUtils.getCurrentVendorId()));
    }

    @GetMapping("/orders")
    public ApiResponse<OrderReport> getOrderReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(reportService.getOrderReport(SecurityUtils.getCurrentVendorId(), startDate, endDate));
    }

    @GetMapping("/popular-items")
    public ApiResponse<PopularItemReport> getPopularItems(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(reportService.getPopularItems(SecurityUtils.getCurrentVendorId(), startDate, endDate));
    }

    @GetMapping("/peak-hours")
    public ApiResponse<PeakHourReport> getPeakHours(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(reportService.getPeakHours(SecurityUtils.getCurrentVendorId(), startDate, endDate));
    }

    @GetMapping("/queue-stats")
    public ApiResponse<QueueStatsReport> getQueueStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(reportService.getQueueStats(SecurityUtils.getCurrentVendorId(), startDate, endDate));
    }

    @GetMapping("/summary")
    public ApiResponse<SummaryReport> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(reportService.getSummary(SecurityUtils.getCurrentVendorId(), startDate, endDate));
    }
}
