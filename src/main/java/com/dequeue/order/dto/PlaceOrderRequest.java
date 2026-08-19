package com.dequeue.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class PlaceOrderRequest {
    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;
    private String customerNote;
    private String customOrderText;
    private Double customerLatitude;
    private Double customerLongitude;
    private String sessionId;
    private java.util.Map<String, String> metadata;
}
