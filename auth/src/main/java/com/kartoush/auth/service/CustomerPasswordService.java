package com.kartoush.auth.service;

import com.kartoush.auth.domain.CustomerPassword;
import com.kartoush.platform.types.CustomerId;

import java.util.Optional;

public interface CustomerPasswordService {

    Optional<CustomerPassword> findByCustomerId(CustomerId customerId);

    CustomerPassword setInitialPassword(CustomerId customerId, String rawPassword);
}
