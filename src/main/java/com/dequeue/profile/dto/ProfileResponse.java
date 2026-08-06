package com.dequeue.profile.dto;

import lombok.Data;
import com.dequeue.profile.entity.SocialLinks;
import java.time.Instant;

@Data
public class ProfileResponse {
    private String id;
    private String vendorId;
    private String shopName;
    private String ownerName;
    private String description;
    private SocialLinks socialLinks;
    private String logoUrl;
    private String bannerUrl;
    private Instant updatedAt;
}

