package com.kartoush.customer.exception;

public class TermsOfServiceVersionAlreadyScheduledException extends RuntimeException {

    public TermsOfServiceVersionAlreadyScheduledException(final String termsOfServiceId) {
        super("Another Terms of Service version is already scheduled: " + termsOfServiceId);
    }
}
