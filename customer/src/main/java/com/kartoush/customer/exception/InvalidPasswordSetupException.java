package com.kartoush.customer.exception;

import com.kartoush.platform.types.CustomerStatus;

public class InvalidPasswordSetupException extends RuntimeException {

    public InvalidPasswordSetupException(final CustomerStatus status) {
        super("Customer cannot complete password setup while in " + status + " status");
    }
}
