package com.kartoush.auth.exception;

public class PasswordSetupTokenExpiredException extends RuntimeException {

    public PasswordSetupTokenExpiredException(final String customerId) {
        super("Password setup token is expired for customer id: " + customerId);
    }
}
