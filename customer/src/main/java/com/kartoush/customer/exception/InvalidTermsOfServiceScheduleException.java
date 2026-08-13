package com.kartoush.customer.exception;

import java.time.Instant;

public class InvalidTermsOfServiceScheduleException extends RuntimeException {

    public static final String MESSAGE_PREFIX =
        "Terms of Service can only be scheduled for a future effectiveAt: ";

    public InvalidTermsOfServiceScheduleException(final Instant effectiveAt) {
        super(MESSAGE_PREFIX + effectiveAt);
    }
}
