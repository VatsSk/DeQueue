package com.dequeue.settings.service;

import com.dequeue.settings.dto.*;

public interface SettingsService {
    SettingsResponse getSettings(String vendorId);
    SettingsResponse updateAllSettings(String vendorId, SettingsResponse request);
    SettingsResponse updateOrderSettings(String vendorId, UpdateOrderSettingsRequest request);
    SettingsResponse updateQueueSettings(String vendorId, UpdateQueueSettingsRequest request);
    SettingsResponse updateNotificationSettings(String vendorId, UpdateNotificationSettingsRequest request);
    SettingsResponse updateDisplaySettings(String vendorId, UpdateDisplaySettingsRequest request);
}
