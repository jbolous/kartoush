package com.kartoush.customer.exception;

import java.time.Instant;

public class InvalidTermsOfServiceScheduleException extends RuntimeException {

    public InvalidTermsOfServiceScheduleException(final Instant effectiveAt) {
        super("Terms of Service can only be scheduled for a future effectiveAt: " + effectiveAt);
    }
}
