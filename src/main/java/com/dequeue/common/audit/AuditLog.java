package com.dequeue.common.audit;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {
    @Id
    private String id;
    
    @Indexed
    private String vendorId;
    private String staffId;
    private String staffName;
    private String action;
    private String entityType;
    private String entityId;
    private String details;
    private String ipAddress;
    
    @Indexed
    @CreatedDate
    private Instant timestamp;
}
