package com.dequeue.reports.service;

import com.dequeue.reports.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    
    @Override
    public TodayReport getTodayReport(String vendorId) {
        TodayReport report = new TodayReport();
        report.setDate(LocalDate.now());
        report.setTotalRevenue(BigDecimal.ZERO);
        return report;
    }
    
    @Override
    public OrderReport getOrderReport(String vendorId, LocalDate startDate, LocalDate endDate) {
        OrderReport report = new OrderReport();
        report.setTotalRevenue(BigDecimal.ZERO);
        report.setAverageOrderValue(BigDecimal.ZERO);
        report.setByStatus(new HashMap<>());
        return report;
    }
    
    @Override
    public PopularItemReport getPopularItems(String vendorId, LocalDate startDate, LocalDate endDate) {
        PopularItemReport report = new PopularItemReport();
        report.setItems(new ArrayList<>());
        return report;
    }
    
    @Override
    public PeakHourReport getPeakHours(String vendorId, LocalDate startDate, LocalDate endDate) {
        PeakHourReport report = new PeakHourReport();
        report.setHours(new ArrayList<>());
        return report;
    }
    
    @Override
    public QueueStatsReport getQueueStats(String vendorId, LocalDate startDate, LocalDate endDate) {
        return new QueueStatsReport();
    }
    
    @Override
    public SummaryReport getSummary(String vendorId, LocalDate startDate, LocalDate endDate) {
        SummaryReport summary = new SummaryReport();
        summary.setOrderReport(getOrderReport(vendorId, startDate, endDate));
        summary.setPopularItemReport(getPopularItems(vendorId, startDate, endDate));
        summary.setPeakHourReport(getPeakHours(vendorId, startDate, endDate));
        summary.setQueueStatsReport(getQueueStats(vendorId, startDate, endDate));
        return summary;
    }
}
