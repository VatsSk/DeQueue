package com.dequeue.queue.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.queue.dto.QueuePositionResponse;
import com.dequeue.queue.dto.QueueStateResponse;
import com.dequeue.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/api/v1/public/queue")
@RequiredArgsConstructor
public class PublicQueueController {
    private final QueueService queueService;

    @GetMapping("/{vendorCode}")
    public ApiResponse<QueueStateResponse> getLiveQueue(@PathVariable String vendorCode) {
        return ApiResponse.success(queueService.getQueueStateByVendorCode(vendorCode));
    }

    @GetMapping("/{vendorCode}/position/{queueNumber}")
    public ApiResponse<QueuePositionResponse> getPosition(@PathVariable String vendorCode, @PathVariable String queueNumber) {
        return ApiResponse.success(queueService.getPosition(vendorCode, queueNumber));
    }

    @GetMapping("/{vendorCode}/poll")
    public DeferredResult<ApiResponse<QueueStateResponse>> pollQueue(@PathVariable String vendorCode) {
        DeferredResult<ApiResponse<QueueStateResponse>> deferredResult = new DeferredResult<>(30000L);
        deferredResult.setResult(ApiResponse.success(queueService.getQueueStateByVendorCode(vendorCode)));
        return deferredResult;
    }
}
