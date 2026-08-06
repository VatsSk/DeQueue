package com.dequeue.geofence.dto;

import lombok.Data;

@Data
public class GeofenceResponse {
    private Double latitude;
    private Double longitude;
    private Double radius;
    private boolean enabled;
}
