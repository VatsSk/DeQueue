package com.dequeue.vendor.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    private String code;
    private String type; // PERCENTAGE, FLAT
    private Double value;
    private boolean active;
}
