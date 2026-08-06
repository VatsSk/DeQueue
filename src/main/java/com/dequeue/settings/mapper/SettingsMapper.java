package com.dequeue.settings.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import com.dequeue.settings.dto.*;
import com.dequeue.settings.entity.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SettingsMapper {
    SettingsResponse toResponse(VendorSetting settings);
    
    void updateOrderSettings(UpdateOrderSettingsRequest request, @MappingTarget OrderSettings settings);
    void updateQueueSettings(UpdateQueueSettingsRequest request, @MappingTarget QueueSettings settings);
    void updateNotificationSettings(UpdateNotificationSettingsRequest request, @MappingTarget NotificationSettings settings);
    void updateDisplaySettings(UpdateDisplaySettingsRequest request, @MappingTarget DisplaySettings settings);
}
