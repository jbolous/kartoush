package com.kartoush.customer.service;

import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;

public record ActivationEmailDelivery(
    CustomerId customerId,
    Email email,
    String rawToken) {
}
