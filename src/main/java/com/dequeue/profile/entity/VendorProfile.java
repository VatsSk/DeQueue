package com.dequeue.profile.entity;

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
@Document(collection = "vendor_profiles")
public class VendorProfile {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String vendorId;
    
    private String shopName;
    private String ownerName;
    private String description;
    private String logo;
    private String banner;
    private List<String> photos;
    private SocialLinks socialLinks;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}
