package com.kartoush.auth.exception;

public class PasswordResetTokenExpiredException extends RuntimeException {

    public PasswordResetTokenExpiredException(final String customerId) {
        super("Password reset token is expired for customer id: " + customerId);
    }
}
