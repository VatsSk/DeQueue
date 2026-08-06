package com.dequeue.profile.dto;

import lombok.Data;
import com.dequeue.profile.entity.SocialLinks;

@Data
public class UpdateProfileRequest {
    private String shopName;
    private String ownerName;
    private String description;
    private SocialLinks socialLinks;
}
