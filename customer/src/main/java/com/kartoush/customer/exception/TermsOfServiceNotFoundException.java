package com.kartoush.customer.exception;

public class TermsOfServiceNotFoundException extends RuntimeException {

    public TermsOfServiceNotFoundException(final String termsOfServiceId) {
        super("Terms of Service not found for id: " + termsOfServiceId);
    }
}
