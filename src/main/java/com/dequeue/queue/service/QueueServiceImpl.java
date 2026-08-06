package com.dequeue.queue.service;

import com.dequeue.queue.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {
    
    private final QueueCacheService cacheService;
    
    @Override
    public QueueStateResponse getQueueState(String vendorId) {
        QueueStateResponse response = new QueueStateResponse();
        response.setLastUpdated(Instant.now());
        response.setCurrentlyServing(cacheService.getCurrentServing(vendorId));
        response.setPending(new ArrayList<>());
        response.setPreparing(new ArrayList<>());
        response.setReady(new ArrayList<>());
        response.setQueueLength(cacheService.getQueueLength(vendorId));
        return response;
    }
    
    @Override
    public QueueStateResponse getQueueStateByVendorCode(String vendorCode) {
        return new QueueStateResponse();
    }
    
    @Override
    public String getCurrentServing(String vendorId) {
        return cacheService.getCurrentServing(vendorId);
    }
    
    @Override
    public String moveToNext(String vendorId) {
        return null;
    }
    
    @Override
    public QueueStatsResponse getQueueStats(String vendorId) {
        return new QueueStatsResponse();
    }
    
    @Override
    public void resetQueue(String vendorId) {
        cacheService.resetDailyQueue(vendorId);
    }
    
    @Override
    public QueuePositionResponse getPosition(String vendorCode, String queueNumber) {
        return new QueuePositionResponse();
    }
}

