package com.kartoush.notification.sms;

import com.kartoush.platform.types.PhoneNumber;

import java.util.Objects;

public record SmsMessage(PhoneNumber recipient, String body) {
    public SmsMessage {
        Objects.requireNonNull(recipient, "recipient is required");
        if (!recipient.value().matches("\\+[1-9][0-9]{6,14}")) {
            throw new IllegalArgumentException("recipient must use international E.164 format");
        }
        if (body == null || body.isBlank() || body.length() > 1600) {
            throw new IllegalArgumentException("SMS body must contain between 1 and 1600 characters");
        }
    }

    @Override
    public String toString() {
        return "SmsMessage[recipient=[REDACTED], body=[REDACTED]]";
    }
}
