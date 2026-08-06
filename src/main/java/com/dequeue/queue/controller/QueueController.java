package com.dequeue.queue.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.queue.dto.QueueStateResponse;
import com.dequeue.queue.dto.QueueStatsResponse;
import com.dequeue.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class QueueController {
    private final QueueService queueService;

    @GetMapping
    public ApiResponse<QueueStateResponse> getQueueState() {
        return ApiResponse.success(queueService.getQueueState(SecurityUtils.getCurrentVendorId()));
    }

    @GetMapping("/current")
    public ApiResponse<String> getCurrentOrder() {
        return ApiResponse.success(queueService.getCurrentServing(SecurityUtils.getCurrentVendorId()));
    }

    @PatchMapping("/next")
    public ApiResponse<String> moveToNext() {
        return ApiResponse.success(queueService.moveToNext(SecurityUtils.getCurrentVendorId()));
    }

    @GetMapping("/stats")
    public ApiResponse<QueueStatsResponse> getQueueStats() {
        return ApiResponse.success(queueService.getQueueStats(SecurityUtils.getCurrentVendorId()));
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> resetQueue() {
        queueService.resetQueue(SecurityUtils.getCurrentVendorId());
        return ApiResponse.success(null);
    }
}
