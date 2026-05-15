package com.kartoush.notification.email.provider.mailtrap;

import com.kartoush.notification.email.config.EmailDeliveryProperties;
import com.kartoush.notification.email.delivery.EmailDeliveryException;
import io.mailtrap.client.MailtrapClient;
import io.mailtrap.config.MailtrapConfig;
import io.mailtrap.factory.MailtrapClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class MailtrapEmailConfiguration {

    @Bean
    MailtrapClient mailtrapSdkClient(final EmailDeliveryProperties properties) {
        try {
            final EmailDeliveryProperties.Mailtrap mailtrapProperties = properties.getMailtrap();
            final MailtrapConfig config = new MailtrapConfig.Builder()
                .token(mailtrapProperties.getApiToken())
                .sandbox(true)
                .inboxId(mailtrapProperties.getInboxId())
                .connectionTimeout(Duration.ofSeconds(10))
                .build();

            return MailtrapClientFactory.createMailtrapClient(config);
        }
        catch (final RuntimeException exception) {
            throw new EmailDeliveryException("mailtrap", "Mailtrap email delivery failed", exception);
        }
    }

    @Bean
    MailtrapEmailClient mailtrapEmailClient(final MailtrapClient mailtrapSdkClient) {
        return new MailtrapEmailClient(mailtrapSdkClient);
    }
}
