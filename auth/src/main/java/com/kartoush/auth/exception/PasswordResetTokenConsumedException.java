package com.kartoush.auth.exception;

public class PasswordResetTokenConsumedException extends RuntimeException {

    public PasswordResetTokenConsumedException(final String customerId) {
        super("Password reset token has already been consumed for customer id: " + customerId);
    }
}
