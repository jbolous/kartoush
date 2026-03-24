package com.kartoush.customer.internal.facade;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.UpdateCustomerRequest;
import com.kartoush.customer.internal.validation.CreateCustomerRequestValidator;
import com.kartoush.customer.internal.validation.UpdateCustomerRequestValidator;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.ulid.UlidGenerator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultCustomerFacade implements CustomerFacade {

    /* TODO(#82): Move credential handling to authentication module.
        Customer should not own passwordHash long-term. */
    private static final String TEMPORARY_PASSWORD_HASH = "TEMPORARY_PASSWORD_HASH";

    private final CustomerService customerService;
    private final UlidGenerator ulidGenerator;
    private final CreateCustomerRequestValidator createCustomerRequestValidator;
    private final UpdateCustomerRequestValidator updateCustomerRequestValidator;

    public DefaultCustomerFacade(
            final CustomerService customerService,
            final UlidGenerator ulidGenerator,
            final CreateCustomerRequestValidator createCustomerRequestValidator,
            final UpdateCustomerRequestValidator updateCustomerRequestValidator) {
        this.customerService = customerService;
        this.ulidGenerator = ulidGenerator;
        this.createCustomerRequestValidator = createCustomerRequestValidator;
        this.updateCustomerRequestValidator = updateCustomerRequestValidator;
    }

    @Override
    public List<CustomerView> getCustomers() {
        return customerService
                .getActiveCustomers()
                .stream()
                .map(this::toCustomerView)
                .toList();
    }

    @Override
    public CustomerView createCustomer(final CreateCustomerRequest request) {
        createCustomerRequestValidator.validate(request);

        final CustomerProfile profile = buildCustomerProfile(request);

        final Customer customer = Customer.createNew(
                CustomerId.newId(ulidGenerator),
                profile,
                new Email(request.email()),
                TEMPORARY_PASSWORD_HASH);

        final var savedCustomer = customerService.createCustomer(customer);
        return toCustomerView(savedCustomer);
    }

    @Override
    public CustomerView getCustomer(final String customerId) {
        return customerService.getCustomerById(customerId)
                .map(this::toCustomerView)
                .orElseThrow( () -> new CustomerNotFoundException(customerId));
    }

    @Override
    public CustomerView updateCustomer(String customerId, UpdateCustomerRequest request) {

        updateCustomerRequestValidator.validate(request);

        CustomerProfile profile = buildCustomerProfile(request);
        Customer savedCustomer = customerService.updateCustomer(customerId, profile);

        return toCustomerView(savedCustomer);
    }

    @Override
    public CustomerView activateCustomer(final String customerId) {
        final Customer customer = customerService.activateCustomer(customerId);
        return toCustomerView(customer);
    }

    @Override
    public CustomerView reactivateCustomer(String customerId) {
        Customer reactiveCustomer = customerService.reactivateCustomer(customerId);

        return toCustomerView(reactiveCustomer);
    }

    @Override
    public CustomerView reactivateCustomer(Email email) {
        Customer reactivatedCustomer = customerService.reactivateCustomerByEmail(email);

        return toCustomerView(reactivatedCustomer);
    }

    @Override
    public void deleteCustomer(String customerId) {
        customerService.deleteCustomer(CustomerId.of(customerId));
    }

    private CustomerView toCustomerView(final Customer customer) {
        return new CustomerView(
                customer.getId().value(),
                customer.getProfile().firstName(),
                customer.getProfile().lastName(),
                customer.getEmail().value(),
                customer.getProfile().phoneNumber(),
                customer.getStatus());
    }

    private CustomerProfile buildCustomerProfile(final UpdateCustomerRequest request) {
        return CustomerProfile.of(
                request.firstName(),
                request.lastName(),
                request.phoneNumber());
    }

    private CustomerProfile buildCustomerProfile(final CreateCustomerRequest request) {
        return CustomerProfile.of(
                request.firstName(),
                request.lastName(),
                request.phoneNumber());
    }
}
