package com.kartoush.auth.service.impl;

import com.kartoush.auth.service.PasswordResetEmailService;
import com.kartoush.platform.types.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NoOpPasswordResetEmailService implements PasswordResetEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(NoOpPasswordResetEmailService.class);

    @Override
    public void sendPasswordResetEmail(final Email email, final String rawToken) {
        LOG.warn(
            "Password reset email delivery requested for email={} but no concrete email delivery provider is configured",
            email.value()
        );
    }
}
