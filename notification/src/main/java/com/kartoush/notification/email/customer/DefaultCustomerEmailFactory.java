package com.kartoush.notification.email.customer;

import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.EmailMessageType;
import com.kartoush.notification.email.config.CustomerEmailProperties;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class DefaultCustomerEmailFactory implements CustomerEmailFactory {

    private final CustomerEmailProperties properties;

    public DefaultCustomerEmailFactory(final CustomerEmailProperties properties) {
        this.properties = properties;
    }

    @Override
    public EmailMessage newActivationEmail(final Email recipient, final CustomerId customerId, final String rawActivationToken) {
        final String actionUrl = properties.getActivationBaseUrl() + "?customerId=" + encode(customerId.value()) + "&token=" + encode(
            rawActivationToken);

        final String activationBody = String.format(
            "Welcome to Kartoush.%n%n"
                + "Activate your account using the link below:%n"
                + "%s",
            actionUrl).trim();

        return new EmailMessage(EmailMessageType.CUSTOMER_ACTIVATION, recipient, new Email(properties.getSenderAddress()),
            properties.getSenderName(), "Activate your Kartoush account", activationBody, actionUrl);
    }

    @Override
    public EmailMessage newPasswordResetEmail(final Email recipient, final String rawResetToken) {
        final String actionUrl = properties.getPasswordResetBaseUrl() + "?email=" + encode(recipient.value()) + "&token=" + encode(
            rawResetToken);

        final String passwordResetBody = String.format(
            "We received a request to reset your Kartoush password.%n%n"
                + "Use the link below to continue:%n"
                + "%s",
            actionUrl).trim();

        return new EmailMessage(EmailMessageType.CUSTOMER_PASSWORD_RESET, recipient, new Email(properties.getSenderAddress()),
            properties.getSenderName(), "Reset your Kartoush password", passwordResetBody, actionUrl);
    }

    private String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
