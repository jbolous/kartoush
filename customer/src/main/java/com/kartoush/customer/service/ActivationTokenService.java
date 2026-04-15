package com.kartoush.customer.service;

import com.kartoush.customer.domain.ActivationToken;
import com.kartoush.platform.types.CustomerId;

public interface ActivationTokenService {
    IssuedActivationToken createFor(CustomerId customerId);

    ActivationToken validate(CustomerId customerId, String rawToken);

    ActivationToken consume(ActivationToken activationToken);

    IssuedActivationToken resendFor(CustomerId customerId);
}
