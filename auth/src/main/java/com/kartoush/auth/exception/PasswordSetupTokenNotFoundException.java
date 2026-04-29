package com.kartoush.auth.exception;

public class PasswordSetupTokenNotFoundException extends RuntimeException {

    public PasswordSetupTokenNotFoundException(final String customerId) {
        super("Password setup token not found for customer id: " + customerId);
    }
}
