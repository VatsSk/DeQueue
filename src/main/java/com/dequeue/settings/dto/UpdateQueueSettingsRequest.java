package com.dequeue.settings.dto;

import lombok.Data;

@Data
public class UpdateQueueSettingsRequest {
    private int maxQueueSize;
    private int estimatedTimePerOrderMinutes;
    private boolean autoPauseWhenFull;
}
