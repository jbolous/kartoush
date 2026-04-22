package com.kartoush.customer.exception;

public class TermsOfServiceVersionNotFoundException extends RuntimeException {

    public TermsOfServiceVersionNotFoundException(final String version) {
        super("Terms of Service not found for version: " + version);
    }
}
