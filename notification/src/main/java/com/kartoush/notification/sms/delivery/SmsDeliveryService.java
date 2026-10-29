package com.kartoush.notification.sms.delivery;

import com.kartoush.notification.sms.SmsMessage;

public interface SmsDeliveryService {
    void send(SmsMessage message);
}
