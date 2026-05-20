package com.kartoush.customer.facade;

import com.kartoush.customer.facade.model.CustomerAuthCandidateView;
import com.kartoush.platform.types.Email;

import java.util.Optional;

public interface CustomerAuthenticationFacade {

    Optional<CustomerAuthCandidateView> findAuthCandidateByEmail(Email email);
}
