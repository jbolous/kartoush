package com.kartoush.customer.exception;

public class ActivationTokenConsumedException extends RuntimeException {
    public ActivationTokenConsumedException(String customerId) {
        super("Activation token has already been consumed for customer id: " + customerId);
    }
}
