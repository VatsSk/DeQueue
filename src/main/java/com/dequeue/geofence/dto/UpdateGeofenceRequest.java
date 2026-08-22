package com.dequeue.geofence.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Data
public class UpdateGeofenceRequest {
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
    
    @NotNull
    @Min(0)
    private Double radius;
    
    private boolean enabled;
}
