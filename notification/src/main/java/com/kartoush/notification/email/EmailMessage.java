package com.kartoush.notification.email;

import com.kartoush.platform.types.Email;

import java.util.Objects;

public record EmailMessage(
    EmailMessageType type,
    Email recipient,
    Email senderAddress,
    String senderName,
    String subject,
    String textBody,
    String actionUrl,
    String htmlBody
) {

    public EmailMessage {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(senderAddress, "senderAddress must not be null");
        senderName = requireNonBlank(senderName, "senderName must not be blank");
        subject = requireNonBlank(subject, "subject must not be blank");
        textBody = requireNonBlank(textBody, "textBody must not be blank");
        actionUrl = requireNonBlank(actionUrl, "actionUrl must not be blank");
        if (htmlBody != null && htmlBody.isBlank()) {
            throw new IllegalArgumentException("htmlBody must not be blank when provided");
        }
        if (htmlBody != null) {
            htmlBody = htmlBody.trim();
        }
    }

    public EmailMessage(
        final EmailMessageType type,
        final Email recipient,
        final Email senderAddress,
        final String senderName,
        final String subject,
        final String textBody,
        final String actionUrl
    ) {
        this(type, recipient, senderAddress, senderName, subject, textBody, actionUrl, null);
    }

    private static String requireNonBlank(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}
