package com.kartoush.customer.exception;

public class NoDueScheduledTermsOfServiceException extends RuntimeException {

    public NoDueScheduledTermsOfServiceException() {
        super("No due scheduled Terms of Service found");
    }
}
