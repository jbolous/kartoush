package com.kartoush.notification.email.config;

import com.kartoush.notification.email.delivery.EmailDeliveryProvider;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.notification.email.delivery.NoOpEmailDeliveryService;
import com.kartoush.notification.email.provider.brevo.BrevoEmailDeliveryService;
import com.kartoush.notification.email.provider.brevo.DefaultBrevoEmailApiClient;
import com.kartoush.notification.email.provider.mailtrap.DefaultMailtrapEmailApiClient;
import com.kartoush.notification.email.http.DefaultNotificationHttpClient;
import com.kartoush.notification.email.provider.mailtrap.MailtrapEmailDeliveryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class EmailDeliveryConfiguration {

    @Bean
    EmailDeliveryService emailDeliveryService(final EmailDeliveryProperties properties) {
        if (!properties.isEnabled() || properties.getProvider() == EmailDeliveryProvider.NOOP) {
            return new NoOpEmailDeliveryService();
        }

        return switch (properties.getProvider()) {
            case MAILTRAP -> new MailtrapEmailDeliveryService(
                new DefaultMailtrapEmailApiClient(
                    new DefaultNotificationHttpClient(HttpClient.newHttpClient()),
                    properties.getMailtrap()
                )
            );
            case BREVO -> new BrevoEmailDeliveryService(
                new DefaultBrevoEmailApiClient(
                    new DefaultNotificationHttpClient(HttpClient.newHttpClient()),
                    properties.getBrevo()
                )
            );
            case NOOP -> new NoOpEmailDeliveryService();
        };
    }
}
