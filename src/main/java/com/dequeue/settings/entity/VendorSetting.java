package com.dequeue.settings.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vendor_settings")
public class VendorSetting {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String vendorId;
    
    @Builder.Default
    private OrderSettings orderSettings = new OrderSettings();
    @Builder.Default
    private QueueSettings queueSettings = new QueueSettings();
    @Builder.Default
    private NotificationSettings notificationSettings = new NotificationSettings();
    @Builder.Default
    private DisplaySettings displaySettings = new DisplaySettings();
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}
