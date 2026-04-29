package com.kartoush.auth.exception;

public class CustomerPasswordAlreadyExistsException extends RuntimeException {

    public CustomerPasswordAlreadyExistsException(final String customerId) {
        super("Customer password already exists for customer id: " + customerId);
    }
}
