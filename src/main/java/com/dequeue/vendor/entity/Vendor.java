package com.dequeue.vendor.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vendors")
public class Vendor {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String vendorCode;
    
    private String shopName;
    private String ownerName;
    
    @Indexed(unique = true)
    private String email;
    
    private String phone;
    private String logo;
    private String banner;
    private Address address;
    private List<BusinessHour> businessHours;
    private ShopStatus shopStatus;
    private GeoLocation geoLocation;
    private Double geoRadius;
    private VendorSettings settings;
    private Subscription subscription;
    
    @Builder.Default
    private boolean active = true;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}
