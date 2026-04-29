package com.kartoush.auth.exception;

public class PasswordSetupTokenConsumedException extends RuntimeException {

    public PasswordSetupTokenConsumedException(final String customerId) {
        super("Password setup token has already been consumed for customer id: " + customerId);
    }
}
