package com.kartoush.platform.jobs;

public class JobSchedulingException extends RuntimeException {

    public JobSchedulingException(final String message) {
        super(message);
    }

    public JobSchedulingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
