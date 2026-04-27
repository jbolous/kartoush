package com.kartoush.auth.service;

import com.kartoush.auth.domain.CustomerCredential;
import com.kartoush.platform.types.CustomerId;

import java.util.Optional;

public interface CustomerCredentialService {

    Optional<CustomerCredential> findByCustomerId(CustomerId customerId);

    CustomerCredential createInitialCredential(CustomerId customerId, String passwordHash);
}
