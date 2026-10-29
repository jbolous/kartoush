package com.kartoush.notification.sms.delivery;

import com.kartoush.notification.sms.SmsMessage;

public class NoOpSmsDeliveryService implements SmsDeliveryService {
    @Override
    public void send(final SmsMessage message) {
        // SMS delivery is disabled; do not contact a provider or log message contents.
    }
}
