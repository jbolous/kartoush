package com.kartoush.auth.exception;

public class PasswordResetTokenNotFoundException extends RuntimeException {

    public PasswordResetTokenNotFoundException(final String customerId) {
        super("Password reset token not found for customer id: " + customerId);
    }
}
