package com.dequeue.reports.dto;

import lombok.Data;

@Data
public class SummaryReport {
    private OrderReport orderReport;
    private PopularItemReport popularItemReport;
    private PeakHourReport peakHourReport;
    private QueueStatsReport queueStatsReport;
}
