package com.dequeue.vendor.dto;

import lombok.Data;
import com.dequeue.vendor.entity.ShopStatus;
import jakarta.validation.constraints.NotNull;

@Data
public class ShopStatusRequest {
    @NotNull(message = "Status is required")
    private ShopStatus status;
}
