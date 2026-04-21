package com.kartoush.customer.exception;

public class TermsOfServiceVersionAlreadyExistsException extends RuntimeException {

    public TermsOfServiceVersionAlreadyExistsException(final String version) {
        super("Terms of Service already exists for version: " + version);
    }
}
