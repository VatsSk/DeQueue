package com.dequeue.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private StaffSummary user;
}
