package com.kartoush.customer.facade.model;

import com.kartoush.platform.types.CustomerStatus;

public record CustomerActivationView(
    String customerId,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    CustomerStatus status,
    String passwordSetupToken) {
}
