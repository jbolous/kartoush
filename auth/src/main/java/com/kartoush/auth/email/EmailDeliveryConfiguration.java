package com.kartoush.auth.email;

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
                    new DefaultMailtrapHttpClient(HttpClient.newHttpClient()),
                    properties.getMailtrap()
                )
            );
            case BREVO -> new BrevoEmailDeliveryService(
                new DefaultBrevoEmailApiClient(
                    new DefaultBrevoHttpClient(HttpClient.newHttpClient()),
                    properties.getBrevo()
                )
            );
            case NOOP -> new NoOpEmailDeliveryService();
        };
    }
}
