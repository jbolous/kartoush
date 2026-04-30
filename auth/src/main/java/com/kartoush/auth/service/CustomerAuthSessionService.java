package com.kartoush.auth.service;

import com.kartoush.auth.domain.IssuedCustomerAccessToken;
import com.kartoush.platform.types.CustomerId;

public interface CustomerAuthSessionService {

    IssuedCustomerAccessToken issueFor(CustomerId customerId);
}
