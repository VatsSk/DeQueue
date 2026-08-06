package com.dequeue.settings.dto;

import lombok.Data;

@Data
public class UpdateDisplaySettingsRequest {
    private boolean showPreparationTime;
    private String currency;
    private String theme;
}
