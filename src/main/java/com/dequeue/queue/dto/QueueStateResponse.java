package com.dequeue.queue.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class QueueStateResponse {
    private String vendorCode;
    private String currentlyServing;
    private List<QueueItem> pending;
    private List<QueueItem> preparing;
    private List<QueueItem> ready;
    private int queueLength;
    private int estimatedWaitTime;
    private Instant lastUpdated;
}

