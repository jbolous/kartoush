package com.kartoush.auth.email;

public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(final String message) {
        super(message);
    }

    public EmailDeliveryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
