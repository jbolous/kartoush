package com.kartoush.auth.exception;

public class PasswordReuseNotAllowedException extends RuntimeException {

    public PasswordReuseNotAllowedException(final String customerId) {
        super("Customer cannot reuse the previous password for customer id: " + customerId);
    }
}
