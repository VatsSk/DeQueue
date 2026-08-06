package com.dequeue.settings.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSettings {
    @Builder.Default
    private boolean autoAcceptOrders = false;
    @Builder.Default
    private boolean allowCustomOrders = false;
    @Builder.Default
    private int maxActiveOrders = 50;
    @Builder.Default
    private int orderTimeout = 30;
}
