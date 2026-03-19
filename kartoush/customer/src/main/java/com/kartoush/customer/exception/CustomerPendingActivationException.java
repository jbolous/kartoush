package com.kartoush.customer.exception;

public class CustomerPendingActivationException extends RuntimeException {
    public CustomerPendingActivationException(String email) {
        super("Customer with email: " + email + ", has a PENDING status");
    }
}
