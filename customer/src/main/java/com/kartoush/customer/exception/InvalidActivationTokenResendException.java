package com.kartoush.customer.exception;

import com.kartoush.platform.types.CustomerStatus;

public class InvalidActivationTokenResendException extends RuntimeException {
    public InvalidActivationTokenResendException(CustomerStatus status) {
        super("Activation token cannot be resent while customer is in " + status + " status");
    }
}
