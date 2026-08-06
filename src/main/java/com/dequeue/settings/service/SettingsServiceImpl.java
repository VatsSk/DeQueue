package com.dequeue.settings.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.dequeue.settings.dto.*;
import com.dequeue.settings.entity.*;
import com.dequeue.settings.repository.SettingsRepository;
import com.dequeue.settings.mapper.SettingsMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final SettingsRepository settingsRepository;
    private final SettingsMapper settingsMapper;

    private VendorSetting getOrCreateSettings(String vendorId) {
        return settingsRepository.findByVendorId(vendorId).orElseGet(() -> {
            VendorSetting setting = new VendorSetting();
            setting.setVendorId(vendorId);
            setting.setOrderSettings(new OrderSettings());
            setting.setQueueSettings(new QueueSettings());
            setting.setNotificationSettings(new NotificationSettings());
            setting.setDisplaySettings(new DisplaySettings());
            return setting;
        });
    }

    @Override
    public SettingsResponse getSettings(String vendorId) {
        return settingsMapper.toResponse(getOrCreateSettings(vendorId));
    }

    @Override
    @Transactional
    public SettingsResponse updateAllSettings(String vendorId, SettingsResponse request) {
        VendorSetting settings = getOrCreateSettings(vendorId);
        if (request.getOrderSettings() != null) settings.setOrderSettings(request.getOrderSettings());
        if (request.getQueueSettings() != null) settings.setQueueSettings(request.getQueueSettings());
        if (request.getNotificationSettings() != null) settings.setNotificationSettings(request.getNotificationSettings());
        if (request.getDisplaySettings() != null) settings.setDisplaySettings(request.getDisplaySettings());
        
        return settingsMapper.toResponse(settingsRepository.save(settings));
    }

    @Override
    @Transactional
    public SettingsResponse updateOrderSettings(String vendorId, UpdateOrderSettingsRequest request) {
        VendorSetting settings = getOrCreateSettings(vendorId);
        if (settings.getOrderSettings() == null) settings.setOrderSettings(new OrderSettings());
        settingsMapper.updateOrderSettings(request, settings.getOrderSettings());
        return settingsMapper.toResponse(settingsRepository.save(settings));
    }

    @Override
    @Transactional
    public SettingsResponse updateQueueSettings(String vendorId, UpdateQueueSettingsRequest request) {
        VendorSetting settings = getOrCreateSettings(vendorId);
        if (settings.getQueueSettings() == null) settings.setQueueSettings(new QueueSettings());
        settingsMapper.updateQueueSettings(request, settings.getQueueSettings());
        return settingsMapper.toResponse(settingsRepository.save(settings));
    }

    @Override
    @Transactional
    public SettingsResponse updateNotificationSettings(String vendorId, UpdateNotificationSettingsRequest request) {
        VendorSetting settings = getOrCreateSettings(vendorId);
        if (settings.getNotificationSettings() == null) settings.setNotificationSettings(new NotificationSettings());
        settingsMapper.updateNotificationSettings(request, settings.getNotificationSettings());
        return settingsMapper.toResponse(settingsRepository.save(settings));
    }

    @Override
    @Transactional
    public SettingsResponse updateDisplaySettings(String vendorId, UpdateDisplaySettingsRequest request) {
        VendorSetting settings = getOrCreateSettings(vendorId);
        if (settings.getDisplaySettings() == null) settings.setDisplaySettings(new DisplaySettings());
        settingsMapper.updateDisplaySettings(request, settings.getDisplaySettings());
        return settingsMapper.toResponse(settingsRepository.save(settings));
    }
}
