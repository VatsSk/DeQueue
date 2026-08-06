package com.dequeue.common.event;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueUpdateEvent {
    private String vendorId;
    private String queueNumber;
    private String action;
    private int queueLength;
    private String currentlyServing;
    private Instant timestamp;
}
