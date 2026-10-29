package com.kartoush.notification.sms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "kartoush.sms.delivery")
public record SmsDeliveryProperties(
    @DefaultValue("false") boolean enabled,
    String accountSid,
    String authToken,
    String fromNumber,
    @DefaultValue("10s") Duration timeout
) {
    public SmsDeliveryProperties {
        if (enabled) {
            if (accountSid == null || !accountSid.matches("AC[0-9a-fA-F]{32}")) {
                throw new IllegalArgumentException("kartoush.sms.delivery.account-sid must be a valid Twilio account SID");
            }
            if (authToken == null || authToken.isBlank()) {
                throw new IllegalArgumentException("kartoush.sms.delivery.auth-token is required");
            }
            if (fromNumber == null || !fromNumber.matches("\\+[1-9][0-9]{6,14}")) {
                throw new IllegalArgumentException("kartoush.sms.delivery.from-number must use international E.164 format");
            }
            if (timeout == null || timeout.isZero() || timeout.isNegative()) {
                throw new IllegalArgumentException("kartoush.sms.delivery.timeout must be positive");
            }
        }
    }

    @Override
    public String toString() {
        return "SmsDeliveryProperties[enabled=" + enabled + ", credentials=[REDACTED]]";
    }
}
