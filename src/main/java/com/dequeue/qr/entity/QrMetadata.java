package com.dequeue.qr.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "qr_metadata")
public class QrMetadata {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String vendorId;
    
    @Indexed(unique = true)
    private String vendorCode;
    
    private String qrUrl;
    private String qrImageUrl;
    private Instant generatedAt;
    
    @Builder.Default
    private int downloadCount = 0;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}
