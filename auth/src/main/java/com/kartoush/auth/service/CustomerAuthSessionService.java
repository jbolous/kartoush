package com.kartoush.auth.service;

import com.kartoush.auth.domain.ActiveSession;
import com.kartoush.auth.domain.IssuedCustomerAccessToken;
import com.kartoush.platform.types.CustomerId;

import java.util.Optional;

public interface CustomerAuthSessionService {

    IssuedCustomerAccessToken issueFor(CustomerId customerId);

    Optional<ActiveSession> findActiveCustomerByAccessToken(String accessToken);

    void revokeAllFor(CustomerId customerId);
}
