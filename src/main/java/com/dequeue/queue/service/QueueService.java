package com.dequeue.queue.service;

import com.dequeue.queue.dto.QueuePositionResponse;
import com.dequeue.queue.dto.QueueStateResponse;
import com.dequeue.queue.dto.QueueStatsResponse;

public interface QueueService {
    QueueStateResponse getQueueState(String vendorId);
    QueueStateResponse getQueueStateByVendorCode(String vendorCode);
    String getCurrentServing(String vendorId);
    String moveToNext(String vendorId);
    QueueStatsResponse getQueueStats(String vendorId);
    void resetQueue(String vendorId);
    QueuePositionResponse getPosition(String vendorCode, String queueNumber);
}
