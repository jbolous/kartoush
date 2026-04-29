package com.kartoush.auth.service.impl;

import com.kartoush.auth.domain.CustomerPassword;
import com.kartoush.auth.persistence.entity.CustomerPasswordEntity;
import com.kartoush.auth.persistence.repository.CustomerPasswordRepository;
import com.kartoush.auth.service.CustomerPasswordHasher;
import com.kartoush.auth.service.CustomerPasswordService;
import com.kartoush.platform.types.CustomerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class DefaultCustomerPasswordService implements CustomerPasswordService {

    private final CustomerPasswordRepository customerPasswordRepository;
    private final CustomerPasswordHasher customerPasswordHasher;

    public DefaultCustomerPasswordService(
        final CustomerPasswordRepository customerPasswordRepository,
        final CustomerPasswordHasher customerPasswordHasher) {

        this.customerPasswordRepository = customerPasswordRepository;
        this.customerPasswordHasher = customerPasswordHasher;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerPassword> findByCustomerId(final CustomerId customerId) {

        return customerPasswordRepository.findById(customerId.value())
            .map(this::toDomain);
    }

    @Override
    @Transactional
    public CustomerPassword setInitialPassword(final CustomerId customerId, final String rawPassword) {

        final CustomerPasswordEntity saved = customerPasswordRepository.save(
            CustomerPasswordEntity.create(
                customerId.value(),
                customerPasswordHasher.hash(rawPassword)));

        return toDomain(saved);
    }

    private CustomerPassword toDomain(final CustomerPasswordEntity entity) {
        return new CustomerPassword(
            CustomerId.of(entity.getCustomerId()),
            entity.getPasswordHash()
        );
    }
}
