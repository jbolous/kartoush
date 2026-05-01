package com.kartoush.auth.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class BrevoEmailDeliveryService implements EmailDeliveryService {

    private static final Logger LOG = LoggerFactory.getLogger(BrevoEmailDeliveryService.class);

    private final BrevoEmailApiClient brevoEmailApiClient;

    public BrevoEmailDeliveryService(final BrevoEmailApiClient brevoEmailApiClient) {
        this.brevoEmailApiClient = brevoEmailApiClient;
    }

    @Override
    public void send(final EmailMessage email) {
        try {
            final Optional<String> providerMessageId = brevoEmailApiClient.send(email);
            if (providerMessageId.isPresent()) {
                LOG.info(
                    "Brevo email sent for type={} recipient={} providerMessageId={}",
                    email.type(),
                    email.recipient().value(),
                    providerMessageId.orElseThrow()
                );
                return;
            }

            LOG.info(
                "Brevo email sent for type={} recipient={}",
                email.type(),
                email.recipient().value()
            );
        } catch (final EmailDeliveryException exception) {
            LOG.error(
                "Brevo email delivery failed for type={} recipient={}",
                email.type(),
                email.recipient().value(),
                exception
            );
            throw exception;
        }
    }
}
