package com.kartoush.customer.exception;

import com.kartoush.platform.types.CustomerStatus;

public class InvalidCustomerReactivationException extends RuntimeException {
    public InvalidCustomerReactivationException(CustomerStatus status) {
        super("Customer can only be reactivated from INACTIVE status, but was %s".formatted(status));
    }
}
