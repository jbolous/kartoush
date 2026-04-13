package com.kartoush.customer.exception;

public class ActivationTokenExpiredException extends RuntimeException {
    public ActivationTokenExpiredException(String customerId) {
        super("Activation token is expired for customer id: " + customerId);
    }
}
