package com.kartoush.auth.service.impl;

import com.kartoush.auth.domain.CustomerCredential;
import com.kartoush.auth.persistence.entity.CustomerCredentialEntity;
import com.kartoush.auth.persistence.repository.CustomerCredentialRepository;
import com.kartoush.auth.service.CustomerCredentialService;
import com.kartoush.platform.types.CustomerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class DefaultCustomerCredentialService implements CustomerCredentialService {

    private final CustomerCredentialRepository customerCredentialRepository;

    public DefaultCustomerCredentialService(final CustomerCredentialRepository customerCredentialRepository) {
        this.customerCredentialRepository = customerCredentialRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerCredential> findByCustomerId(final CustomerId customerId) {
        return customerCredentialRepository.findById(customerId.value())
            .map(this::toDomain);
    }

    @Override
    @Transactional
    public CustomerCredential createInitialCredential(final CustomerId customerId, final String passwordHash) {
        final CustomerCredentialEntity saved = customerCredentialRepository.save(
            CustomerCredentialEntity.create(customerId.value(), passwordHash)
        );
        return toDomain(saved);
    }

    private CustomerCredential toDomain(final CustomerCredentialEntity entity) {
        return new CustomerCredential(
            CustomerId.of(entity.getCustomerId()),
            entity.getPasswordHash()
        );
    }
}
