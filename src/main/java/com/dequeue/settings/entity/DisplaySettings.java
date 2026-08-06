package com.dequeue.settings.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisplaySettings {
    @Builder.Default
    private String theme = "dark";
    @Builder.Default
    private String language = "en";
    @Builder.Default
    private boolean showPreparationTime = true;
    @Builder.Default
    private boolean compactView = false;
}
