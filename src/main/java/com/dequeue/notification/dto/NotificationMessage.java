package com.dequeue.notification.dto;

import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
public class NotificationMessage {
    private String type;
    private String vendorId;
    private Map<String, Object> data;
    private Instant timestamp;
}

