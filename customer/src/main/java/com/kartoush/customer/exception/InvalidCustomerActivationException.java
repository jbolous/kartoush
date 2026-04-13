package com.kartoush.customer.exception;

import com.kartoush.platform.types.CustomerStatus;

public class InvalidCustomerActivationException extends RuntimeException {
    public InvalidCustomerActivationException(CustomerStatus status) {
        super("Customer cannot be activated while in " + status + " status");
    }
}
