package com.dequeue.settings.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueSettings {
    @Builder.Default
    private int maxQueueSize = 100;
    @Builder.Default
    private boolean resetQueueDaily = true;
    @Builder.Default
    private String queuePrefix = "Q";
    @Builder.Default
    private boolean showEstimatedTime = true;
}
