package com.kartoush.auth.exception;

public class InvalidPasswordResetException extends RuntimeException {

    public InvalidPasswordResetException(final String customerStatus) {
        super("Customer cannot reset password while in " + customerStatus + " status");
    }
}
