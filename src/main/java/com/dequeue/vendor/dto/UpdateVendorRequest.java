package com.dequeue.vendor.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
public class UpdateVendorRequest {
    @NotBlank(message = "Shop name is required")
    private String shopName;
    
    @NotBlank(message = "Owner name is required")
    private String ownerName;
    
    @NotBlank(message = "Phone is required")
    private String phone;
    
    private AddressDto address;
    private List<BusinessHourDto> businessHours;
}
