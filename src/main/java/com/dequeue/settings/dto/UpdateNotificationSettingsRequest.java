package com.dequeue.settings.dto;

import lombok.Data;

@Data
public class UpdateNotificationSettingsRequest {
    private boolean notifyOnNewOrder;
    private boolean notifyOnOrderCancel;
    private boolean sendSms;
    private boolean sendEmail;
}
