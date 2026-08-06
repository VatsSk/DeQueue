package com.dequeue.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomOrderRequest {
    @NotBlank
    @Size(max = 500)
    private String text;
    private String customerNote;
    private String sessionId;
}
