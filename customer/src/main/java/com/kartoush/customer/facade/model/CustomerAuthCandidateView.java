package com.kartoush.customer.facade.model;

import com.kartoush.platform.types.CustomerStatus;

public record CustomerAuthCandidateView(
    String customerId,
    String email,
    CustomerStatus status
) {
}
