package com.dequeue.vendor.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocation {
    private Double latitude;
    private Double longitude;
}
