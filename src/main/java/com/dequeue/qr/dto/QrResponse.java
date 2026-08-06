package com.dequeue.qr.dto;
import lombok.Data;
import java.time.Instant;

@Data
public class QrResponse {
    private String vendorCode;
    private String qrUrl;
    private String qrImageUrl;
    private Instant generatedAt;
    private Integer downloadCount;
}

