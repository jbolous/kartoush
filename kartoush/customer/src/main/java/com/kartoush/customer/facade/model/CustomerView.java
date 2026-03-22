package com.kartoush.customer.facade.model;

import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;

public record CustomerView(
        String customerId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        CustomerStatus status
) {
    public boolean isActive () {
        return status == CustomerStatus.ACTIVE;
    }
}
