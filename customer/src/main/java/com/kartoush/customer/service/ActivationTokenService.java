package com.kartoush.customer.service;

import com.kartoush.customer.domain.ActivationToken;
import com.kartoush.platform.types.CustomerId;

public interface ActivationTokenService {
    ActivationToken createFor(CustomerId customerId);

    ActivationToken validate(CustomerId customerId, String rawToken);

    ActivationToken consume(ActivationToken activationToken);

    ActivationToken resendFor(CustomerId customerId);
}
