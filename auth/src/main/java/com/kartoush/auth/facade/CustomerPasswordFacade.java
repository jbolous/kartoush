package com.kartoush.auth.facade;

import com.kartoush.auth.domain.CustomerPassword;
import com.kartoush.auth.domain.IssuedPasswordSetupToken;
import com.kartoush.platform.types.CustomerId;

import java.util.Optional;

public interface CustomerPasswordFacade {

    Optional<CustomerPassword> findByCustomerId(CustomerId customerId);

    IssuedPasswordSetupToken issuePasswordSetupToken(CustomerId customerId);

    CustomerPassword setInitialPassword(CustomerId customerId, String rawSetupToken, String rawPassword);
}
