package com.kartoush.customer.exception;

import com.kartoush.platform.types.CustomerStatus;

public class InvalidCustomerStatusForUpdateException extends RuntimeException {
    public InvalidCustomerStatusForUpdateException(CustomerStatus status) {
        super("Customer cannot be updated while in %s status".formatted(status.name()));
    }
}
