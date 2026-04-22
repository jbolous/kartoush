package com.kartoush.customer.exception;

public class CurrentTermsOfServiceNotFoundException extends RuntimeException {

    public CurrentTermsOfServiceNotFoundException() {
        super("Current Terms of Service not found");
    }
}
