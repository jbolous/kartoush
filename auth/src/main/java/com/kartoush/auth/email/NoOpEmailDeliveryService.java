package com.kartoush.auth.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(EmailDeliveryService.class)
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
