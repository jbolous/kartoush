package com.kartoush.notification.email.delivery;

/** A provider response that definitively rejected the request without accepting the email. */
public class EmailDeliveryOutcomeException extends EmailDeliveryException {
    private final boolean retryable;

    public EmailDeliveryOutcomeException(final String provider, final String message, final boolean retryable) {
        super(provider, message);
        this.retryable = retryable;
    }

    public boolean retryable() { return retryable; }
}
