package com.dequeue.settings.dto;

import lombok.Data;
import com.dequeue.settings.entity.*;

@Data
public class SettingsResponse {
    private String id;
    private String vendorId;
    private OrderSettings orderSettings;
    private QueueSettings queueSettings;
    private NotificationSettings notificationSettings;
    private DisplaySettings displaySettings;
}
