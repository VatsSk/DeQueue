package com.dequeue.auth.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document(collection = "refresh_tokens")
public class RefreshToken {
    @Id
    private String id;
    @Indexed(unique = true)
    private String token;
    private String staffId;
    private String vendorId;
    private Instant expiryDate;
    private Instant createdAt;
}
