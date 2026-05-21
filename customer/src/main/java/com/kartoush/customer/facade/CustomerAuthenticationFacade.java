package com.kartoush.customer.facade;

import com.kartoush.customer.facade.model.AuthCandidateView;
import com.kartoush.platform.types.Email;

import java.util.Optional;

public interface CustomerAuthenticationFacade {

    Optional<AuthCandidateView> findAuthCandidateByEmail(Email email);

    Optional<AuthCandidateView> findAuthCandidateById(String customerId);
}
