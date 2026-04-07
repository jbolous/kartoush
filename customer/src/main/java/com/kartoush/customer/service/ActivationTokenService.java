package com.kartoush.customer.service;

import com.kartoush.customer.domain.ActivationToken;
import com.kartoush.platform.types.CustomerId;

public interface ActivationTokenService {
    ActivationToken createFor(CustomerId customerId);
}
