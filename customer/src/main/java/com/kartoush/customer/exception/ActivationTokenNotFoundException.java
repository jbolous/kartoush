package com.kartoush.customer.exception;

public class ActivationTokenNotFoundException extends RuntimeException {
    public ActivationTokenNotFoundException(String customerId) {
        super("Activation token not found for customer id: " + customerId);
    }
}
