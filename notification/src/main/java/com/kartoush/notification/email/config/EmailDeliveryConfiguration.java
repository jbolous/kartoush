package com.kartoush.notification.email.config;

import com.kartoush.notification.email.delivery.EmailDeliveryProvider;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.notification.email.delivery.NoOpEmailDeliveryService;
import com.kartoush.notification.email.provider.brevo.BrevoEmailClient;
import com.kartoush.notification.email.delivery.TrackedEmailDeliveryService;
import com.kartoush.notification.persistence.repository.EmailDeliveryRepository;
import org.springframework.transaction.PlatformTransactionManager;
import com.kartoush.notification.email.http.DefaultNotificationHttpClient;
import com.kartoush.notification.email.provider.mailtrap.MailtrapEmailClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Clock;

@Configuration
public class EmailDeliveryConfiguration {

    @Bean
    EmailDeliveryService emailDeliveryService(
        final EmailDeliveryProperties properties,
        final MailtrapEmailClient mailtrapEmailClient,
        final EmailDeliveryRepository repository,
        final PlatformTransactionManager transactionManager,
        final Clock clock
    ) {
        if (!properties.isEnabled() || properties.getProvider() == EmailDeliveryProvider.NOOP) {
            return new NoOpEmailDeliveryService();
        }

        return switch (properties.getProvider()) {
            case MAILTRAP -> new TrackedEmailDeliveryService("mailtrap", mailtrapEmailClient, repository, transactionManager, clock);
            case BREVO -> new TrackedEmailDeliveryService("brevo",
                new BrevoEmailClient(
                    new DefaultNotificationHttpClient(HttpClient.newHttpClient()),
                    properties.getBrevo()
                ), repository, transactionManager, clock
            );
            case NOOP -> new NoOpEmailDeliveryService();
        };
    }
}
