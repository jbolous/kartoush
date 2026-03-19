package com.kartoush.customer.exception;

public class CustomerDeletedException extends RuntimeException {
    public CustomerDeletedException(String email) {
        super("Customer is DELETED and already exists with email: " + email);
    }
}
