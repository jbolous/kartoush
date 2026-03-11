package com.kartoush.customer.internal.facade;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.internal.validation.CreateCustomerRequestValidator;
import com.kartoush.customer.persistence.mapper.CustomerMapper;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.ulid.UlidGenerator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class DefaultCustomerFacade implements CustomerFacade {

    /* TODO(#82): Move credential handling to authentication module.
        Customer should not own passwordHash long-term. */
    private static final String TEMPORARY_PASSWORD_HASH = "TEMPORARY_PASSWORD_HASH";

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final UlidGenerator ulidGenerator;
    private final CreateCustomerRequestValidator validator;

    public DefaultCustomerFacade(
            final CustomerRepository customerRepository,
            final CustomerMapper customerMapper,
            final UlidGenerator ulidGenerator,
            final CreateCustomerRequestValidator validator) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.ulidGenerator = ulidGenerator;
        this.validator = validator;
    }

    @Override
    public CustomerView createCustomer(final CreateCustomerRequest request) {
        validator.validate(request);

        CustomerProfile profile = CustomerProfile.of(
                request.firstName(),
                request.lastName(),
                request.phoneNumber());

        final Customer customer = Customer.createNew(
                CustomerId.newId(ulidGenerator),
                profile,
                request.email(),
                TEMPORARY_PASSWORD_HASH,
                Instant.now());

        final var savedCustomer = saveCustomer(customer);

        return toCustomerView(savedCustomer);
    }

    @Override
    public Optional<CustomerView> getCustomerById(final CustomerId customerId) {
        return customerRepository.findById(
                CustomerIdEmbeddable.from(customerId))
                .map(customerMapper::toDomain)
                .map(this::toCustomerView);
    }

    private CustomerView toCustomerView(final Customer customer) {
        return new CustomerView(
                customer.getId(),
                customer.getEmail(),
                customer.getProfile().phoneNumber(),
                customer.getProfile().firstName(),
                customer.getProfile().lastName(),
                customer.getStatus());
    }

    private Customer saveCustomer(final Customer customer) {
        final var savedEntity = customerRepository.save(customerMapper.toEntity(customer));
        return customerMapper.toDomain(savedEntity);
    }
}
