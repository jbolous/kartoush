package com.kartoush.customer.service.impl;

import com.kartoush.customer.service.ActivationEmailService;
import com.kartoush.platform.types.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NoOpActivationEmailService implements ActivationEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(NoOpActivationEmailService.class);

    @Override
    public void sendActivationToken(final Email email, final String rawToken) {
        LOG.warn(
            "Activation email delivery requested for email={} but no concrete email delivery provider is configured. " +
                "Development activation token={}",
            email.value(),
            rawToken);
    }
}
