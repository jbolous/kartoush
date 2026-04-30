package com.kartoush.auth.email;

import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class DefaultCustomerEmailFactory implements CustomerEmailFactory {

    private final CustomerTransactionalEmailProperties properties;

    public DefaultCustomerEmailFactory(final CustomerTransactionalEmailProperties properties) {
        this.properties = properties;
    }

    @Override
    public EmailMessage newActivationEmail(
        final Email recipient,
        final CustomerId customerId,
        final String rawActivationToken
    ) {
        final String actionUrl = properties.getActivationBaseUrl()
            + "?customerId=" + encode(customerId.value())
            + "&token=" + encode(rawActivationToken);

        return new EmailMessage(
            EmailMessageType.CUSTOMER_ACTIVATION,
            recipient,
            new Email(properties.getSenderAddress()),
            properties.getSenderName(),
            "Activate your Kartoush account",
            """
                Welcome to Kartoush.

                Activate your account using the link below:
                %s
                """.formatted(actionUrl).trim(),
            actionUrl
        );
    }

    @Override
    public EmailMessage newPasswordResetEmail(final Email recipient, final String rawResetToken) {
        final String actionUrl = properties.getPasswordResetBaseUrl()
            + "?email=" + encode(recipient.value())
            + "&token=" + encode(rawResetToken);

        return new EmailMessage(
            EmailMessageType.CUSTOMER_PASSWORD_RESET,
            recipient,
            new Email(properties.getSenderAddress()),
            properties.getSenderName(),
            "Reset your Kartoush password",
            """
                We received a request to reset your Kartoush password.

                Use the link below to continue:
                %s
                """.formatted(actionUrl).trim(),
            actionUrl
        );
    }

    private String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
