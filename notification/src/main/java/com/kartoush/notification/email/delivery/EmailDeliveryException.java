package com.kartoush.notification.email.delivery;

public class EmailDeliveryException extends RuntimeException {

    private final String provider;

    public EmailDeliveryException(final String provider, final String message) {
        super(message);
        this.provider = provider;
    }

    public EmailDeliveryException(final String provider, final String message, final Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }

    public String provider() {
        return provider;
    }
}
