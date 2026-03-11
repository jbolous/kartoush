package com.kartoush.customer.facade.model;

import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;

public record CustomerView(
        CustomerId customerId,
        String email,
        String phoneNumber,
        String firstName,
        String lastName,
        CustomerStatus status
) {

    public boolean isActive () {
        return status == CustomerStatus.ACTIVE;
    }
}
