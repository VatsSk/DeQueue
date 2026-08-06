package com.dequeue.qr.controller;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.qr.dto.QrGenerateRequest;
import com.dequeue.qr.dto.QrResponse;
import com.dequeue.qr.service.QrService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qr")
@RequiredArgsConstructor
public class QrController {
    private final QrService service;
    
    @GetMapping
    public ApiResponse<QrResponse> get() {
        return ApiResponse.success(service.getQrMetadata());
    }
    
    @PostMapping("/generate")
    public ApiResponse<QrResponse> generate(@RequestBody QrGenerateRequest request) {
        return ApiResponse.success(service.generateQr(request));
    }
    
    @GetMapping("/download")
    public byte[] download() {
        return service.downloadQr();
    }
}

