package com.dequeue.qr.dto;
import lombok.Data;

@Data
public class QrGenerateRequest {
    private int size = 300;
    private String format = "png";
}
