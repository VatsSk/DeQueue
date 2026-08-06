package com.dequeue.profile.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLinks {
    private String website;
    private String instagram;
    private String facebook;
    private String twitter;
}
