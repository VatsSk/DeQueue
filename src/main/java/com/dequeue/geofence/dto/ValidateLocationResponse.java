package com.dequeue.geofence.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class ValidateLocationResponse {
    private boolean withinRange;
    private double distance;
    private double maxRadius;
}
