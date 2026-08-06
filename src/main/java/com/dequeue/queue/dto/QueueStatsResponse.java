package com.dequeue.queue.dto;

import lombok.Data;

@Data
public class QueueStatsResponse {
    private int queueLength;
    private int averageWaitTime;
    private int averagePrepTime;
    private int ordersServedToday;
    private String peakHour;
    private String currentlyServing;
}
