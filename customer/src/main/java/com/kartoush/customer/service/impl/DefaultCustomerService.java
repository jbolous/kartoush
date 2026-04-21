package com.kartoush.customer.service.impl;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.domain.ActivationToken;
import com.kartoush.customer.exception.InvalidCustomerActivationException;
import com.kartoush.customer.exception.InvalidActivationTokenResendException;
import com.kartoush.customer.exception.CustomerAlreadyExistsException;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.exception.CustomerPendingActivationException;
import com.kartoush.customer.exception.InvalidCustomerStatusForUpdateException;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.entity.TermsAcceptanceEntity;
import com.kartoush.customer.persistence.mapper.CustomerMapper;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.customer.persistence.repository.TermsAcceptanceRepository;
import com.kartoush.customer.service.ActivationEmailDelivery;
import com.kartoush.customer.service.ActivationTokenService;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.customer.service.IssuedActivationToken;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kartoush.platform.ulid.UlidGenerator;

@Service
public class DefaultCustomerService implements CustomerService
{
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCustomerService.class);
    private final CustomerRepository customerRepository;
    private final TermsAcceptanceRepository termsAcceptanceRepository;
    private final CustomerMapper customerMapper;
    private final ActivationTokenService activationTokenService;
    private final UlidGenerator ulidGenerator;
    private final Clock clock;

    public DefaultCustomerService(
            final CustomerRepository customerRepository,
            final TermsAcceptanceRepository termsAcceptanceRepository,
            final CustomerMapper customerMapper,
            final ActivationTokenService activationTokenService,
            final UlidGenerator ulidGenerator,
            final Clock clock) {
        this.customerRepository = customerRepository;
        this.termsAcceptanceRepository = termsAcceptanceRepository;
        this.customerMapper = customerMapper;
        this.activationTokenService = activationTokenService;
        this.ulidGenerator = ulidGenerator;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getActiveCustomers() {
        return customerRepository.findByCustomerStatus(CustomerStatus.ACTIVE)
                .stream()
                .map(customerMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerById(final String customerId) {
        return customerRepository.findById(
                CustomerIdEmbeddable.from(customerId))
                .map(customerMapper::toDomain);
    }

    @Override
    @Transactional
    public Customer registerCustomer(final Customer customer, final String termsVersion) {

        final Optional<CustomerEntity> existingCustomer = customerRepository.findByEmail(customer.getEmail().value());

        if (existingCustomer.isPresent()) {
            final String email = existingCustomer.get().getEmail();
            final CustomerStatus status = existingCustomer.get().getCustomerStatus();

            if (status == CustomerStatus.PENDING) {
                throw new CustomerPendingActivationException(email);
            }

            throw new CustomerAlreadyExistsException(email);
        }

        CustomerEntity entity = customerMapper.toEntity(customer);
        LOG.debug("Saving customer entity id={} email={}", entity.getId(), entity.getEmail());

        CustomerEntity savedCustomer = customerRepository.save(entity);

        LOG.debug("Returned from save customer entity id={} email={}", savedCustomer.getId(), savedCustomer.getEmail());

        final TermsAcceptanceEntity termsAcceptance = TermsAcceptanceEntity.of(
            ulidGenerator.next(),
            savedCustomer.getCustomerId(),
            termsVersion,
            Instant.now(clock)
        );
        termsAcceptanceRepository.save(termsAcceptance);

        return customerMapper.toDomain(savedCustomer);
    }

    @Override
    @Transactional
    public Customer updateCustomer(
            final String customerId,
            final CustomerProfile profile) {
        final CustomerEntity customerEntity = customerRepository.findById(CustomerIdEmbeddable.from(customerId))
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        validateCustomerCanBeUpdated(customerEntity);

        final Customer customer = customerMapper.toDomain(customerEntity);
        customer.updateDetails(profile);

        customerMapper.updateEntity(customer, customerEntity);
        final CustomerEntity savedCustomer = customerRepository.save(customerEntity);

        return customerMapper.toDomain(savedCustomer);
    }

    @Override
    @Transactional
    public void deleteCustomer(final CustomerId customerId) {
        final CustomerEntity customerEntity = customerRepository
                .findById(
                        CustomerIdEmbeddable
                                .from(customerId))
                .orElseThrow(() -> new CustomerNotFoundException(customerId.value()));

        final Customer customer = customerMapper.toDomain(customerEntity);

        customer.softDelete();

        customerMapper.updateEntity(customer, customerEntity);

        customerRepository.save(customerEntity);
    }

    @Override
    @Transactional
    public Customer reactivateCustomer(final String customerId) {
        final CustomerEntity customerEntity = customerRepository
                .findById(CustomerIdEmbeddable.from(customerId))
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        final Customer customer = customerMapper.toDomain(customerEntity);

        customer.reactivate();

        customerMapper.updateEntity(customer, customerEntity);

        final CustomerEntity savedCustomer = customerRepository.save(customerEntity);

        return customerMapper.toDomain(savedCustomer);
    }

    @Override
    @Transactional
    public Customer reactivateCustomerByEmail(final Email email) {
        final CustomerEntity customerEntity = customerRepository.findByEmail(email.value())
                .orElseThrow(() -> new CustomerNotFoundException(email.value()));

        final Customer customer = customerMapper.toDomain(customerEntity);

        customer.reactivate();

        customerMapper.updateEntity(customer, customerEntity);

        final CustomerEntity savedCustomer = customerRepository.save(customerEntity);

        return customerMapper.toDomain(savedCustomer);
    }

    @Override
    @Transactional
    public Customer activateCustomer(String customerId, String rawToken) {
        final CustomerEntity customerEntity = customerRepository
                .findById(CustomerIdEmbeddable.from(customerId))
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        final CustomerId customerIdValue = CustomerId.of(customerId);
        final ActivationToken activationToken = activationTokenService.validate(customerIdValue, rawToken);

        final Customer customer = customerMapper.toDomain(customerEntity);

        if (customer.getStatus() != CustomerStatus.PENDING) {
            throw new InvalidCustomerActivationException(customer.getStatus());
        }

        customer.activate();

        customerMapper.updateEntity(customer, customerEntity);

        final CustomerEntity savedCustomer = customerRepository.save(customerEntity);
        activationTokenService.consume(activationToken);

        return customerMapper.toDomain(savedCustomer);
    }

    @Override
    @Transactional
    public ActivationEmailDelivery issueActivationTokenForResend(final String customerId) {
        final CustomerEntity customerEntity = customerRepository
            .findById(CustomerIdEmbeddable.from(customerId))
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        final Customer customer = customerMapper.toDomain(customerEntity);

        if (customer.getStatus() != CustomerStatus.PENDING) {
            throw new InvalidActivationTokenResendException(customer.getStatus());
        }

        final IssuedActivationToken issuedActivationToken = activationTokenService.resendFor(customer.getId());
        return new ActivationEmailDelivery(customer.getEmail(), issuedActivationToken.rawToken());
    }

    private void validateCustomerCanBeUpdated(final CustomerEntity customer) {
        final CustomerStatus status = customer.getCustomerStatus();

        if (status == CustomerStatus.INACTIVE || status == CustomerStatus.DELETED) {
            throw new InvalidCustomerStatusForUpdateException(status);
        }
    }
}
