package com.dequeue.reports.dto;

import lombok.Data;

@Data
public class QueueStatsReport {
    private int averageWaitTime;
    private int averagePrepTime;
    private int maxQueueLength;
    private int totalServed;
}
