package com.dequeue.reports.service;

import com.dequeue.reports.dto.*;
import java.time.LocalDate;

public interface ReportService {
    TodayReport getTodayReport(String vendorId);
    OrderReport getOrderReport(String vendorId, LocalDate startDate, LocalDate endDate);
    PopularItemReport getPopularItems(String vendorId, LocalDate startDate, LocalDate endDate);
    PeakHourReport getPeakHours(String vendorId, LocalDate startDate, LocalDate endDate);
    QueueStatsReport getQueueStats(String vendorId, LocalDate startDate, LocalDate endDate);
    SummaryReport getSummary(String vendorId, LocalDate startDate, LocalDate endDate);
    String getExportCSV(String vendorId, LocalDate startDate, LocalDate endDate);
}
