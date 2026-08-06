package com.dequeue.geofence.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ValidateLocationRequest {
    @NotBlank
    private String vendorCode;
    
    @NotNull
    private Double customerLatitude;
    
    @NotNull
    private Double customerLongitude;
}
