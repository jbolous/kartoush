package com.kartoush.customer.exception;

import com.kartoush.platform.types.CustomerStatus;

public class InvalidCustomerStatusTransitionException extends RuntimeException {
    public InvalidCustomerStatusTransitionException(final CustomerStatus currentStatus,
                                                    final CustomerStatus targetStatus) {
        super("Invalid customer status transition from %s to %s".formatted(currentStatus, targetStatus));
    }
}
