package com.kartoush.auth.email;

import com.kartoush.platform.types.Email;

import java.util.Objects;

public record EmailMessage(
    EmailMessageType type,
    Email recipient,
    Email senderAddress,
    String senderName,
    String subject,
    String textBody,
    String actionUrl
) {

    public EmailMessage {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(senderAddress, "senderAddress must not be null");
        senderName = requireNonBlank(senderName, "senderName must not be blank");
        subject = requireNonBlank(subject, "subject must not be blank");
        textBody = requireNonBlank(textBody, "textBody must not be blank");
        actionUrl = requireNonBlank(actionUrl, "actionUrl must not be blank");
    }

    private static String requireNonBlank(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}
