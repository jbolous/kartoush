package com.kartoush.notification.email.delivery;

import com.kartoush.notification.email.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpEmailDeliveryService implements EmailDeliveryService {

    private static final Logger LOG = LoggerFactory.getLogger(NoOpEmailDeliveryService.class);

    @Override
    public void send(final EmailMessage email) {
        LOG.warn(
            "Transactional email delivery requested for type={} recipient={} but no concrete email delivery provider is configured",
            email.type(),
            email.recipient().value()
        );
    }
}
