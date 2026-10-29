package com.kartoush.notification.sms.config;

import com.kartoush.notification.sms.delivery.NoOpSmsDeliveryService;
import com.kartoush.notification.sms.delivery.SmsDeliveryService;
import com.kartoush.notification.sms.provider.twilio.TwilioSmsDeliveryService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(SmsDeliveryProperties.class)
public class SmsDeliveryConfiguration {
    @Bean
    public SmsDeliveryService smsDeliveryService(final SmsDeliveryProperties properties) {
        if (!properties.enabled()) {
            return new NoOpSmsDeliveryService();
        }
        return new TwilioSmsDeliveryService(HttpClient.newBuilder()
            .connectTimeout(properties.timeout()).followRedirects(HttpClient.Redirect.NEVER).build(), properties);
    }
}
