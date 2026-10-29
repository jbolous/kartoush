package com.kartoush.notification.sms.delivery;

public class SmsDeliveryException extends RuntimeException {
    public SmsDeliveryException(final String message) {
        super(message);
    }

    public SmsDeliveryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
