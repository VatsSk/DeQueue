package com.dequeue.settings.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettings {
    @Builder.Default
    private boolean soundEnabled = true;
    @Builder.Default
    private String soundType = "default";
    @Builder.Default
    private int autoRefreshInterval = 10;
}
