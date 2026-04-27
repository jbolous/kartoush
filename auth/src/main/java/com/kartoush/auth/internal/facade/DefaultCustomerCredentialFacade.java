package com.kartoush.auth.internal.facade;

import com.kartoush.auth.domain.CustomerCredential;
import com.kartoush.auth.facade.CustomerCredentialFacade;
import com.kartoush.auth.service.CustomerCredentialService;
import com.kartoush.platform.types.CustomerId;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DefaultCustomerCredentialFacade implements CustomerCredentialFacade {

    private final CustomerCredentialService customerCredentialService;

    public DefaultCustomerCredentialFacade(final CustomerCredentialService customerCredentialService) {
        this.customerCredentialService = customerCredentialService;
    }

    @Override
    public Optional<CustomerCredential> findByCustomerId(final CustomerId customerId) {
        return customerCredentialService.findByCustomerId(customerId);
    }

    @Override
    public CustomerCredential createInitialCredential(final CustomerId customerId, final String passwordHash) {
        return customerCredentialService.createInitialCredential(customerId, passwordHash);
    }
}
