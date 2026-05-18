package com.kartoush.notification.email.provider.mailtrap;

import com.kartoush.notification.email.client.EmailClient;
import com.kartoush.notification.email.delivery.EmailDeliveryException;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.notification.email.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class MailtrapEmailDeliveryService implements EmailDeliveryService {

    private static final Logger LOG = LoggerFactory.getLogger(MailtrapEmailDeliveryService.class);

    private final EmailClient emailClient;

    public MailtrapEmailDeliveryService(final EmailClient emailClient) {
        this.emailClient = emailClient;
    }

    @Override
    public void send(final EmailMessage email) {
        try {
            final Optional<String> providerMessageId = emailClient.send(email);
            if (providerMessageId.isPresent()) {
                LOG.info(
                    "Mailtrap email sent for type={} recipient={} providerMessageId={}",
                    email.type(),
                    email.recipient().value(),
                    providerMessageId.orElseThrow()
                );
                return;
            }

            LOG.info(
                "Mailtrap email sent for type={} recipient={}",
                email.type(),
                email.recipient().value()
            );
        } catch (final EmailDeliveryException exception) {
            LOG.error(
                "Mailtrap email delivery failed for type={} recipient={}",
                email.type(),
                email.recipient().value(),
                exception
            );
            throw exception;
        }
    }
}
