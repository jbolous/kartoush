package com.kartoush.customer.internal.facade;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.UpdateCustomerInput;
import com.kartoush.customer.internal.validation.CreateCustomerInputValidator;
import com.kartoush.customer.internal.validation.UpdateCustomerInputValidator;
import com.kartoush.customer.service.ActivationEmailDelivery;
import com.kartoush.customer.service.ActivationEmailService;
import com.kartoush.customer.service.ActivationTokenService;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.customer.service.IssuedActivationToken;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.ulid.UlidGenerator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultCustomerFacade implements CustomerFacade {

    /* TODO(#82): Move credential handling to a dedicated authentication module.
     * Customer should not own passwordHash long-term as part of proper domain separation.
     */
    private static final String TEMPORARY_PASSWORD_HASH = "TEMPORARY_PASSWORD_HASH";

    private final CustomerService customerService;
    private final ActivationEmailService activationEmailService;
    private final ActivationTokenService activationTokenService;
    private final UlidGenerator ulidGenerator;
    private final CreateCustomerInputValidator createCustomerInputValidator;
    private final UpdateCustomerInputValidator updateCustomerInputValidator;

    public DefaultCustomerFacade(
            final CustomerService customerService,
            final ActivationEmailService activationEmailService,
            final ActivationTokenService activationTokenService,
            final UlidGenerator ulidGenerator,
            final CreateCustomerInputValidator createCustomerInputValidator,
            final UpdateCustomerInputValidator updateCustomerInputValidator) {
        this.customerService = customerService;
        this.activationEmailService = activationEmailService;
        this.activationTokenService = activationTokenService;
        this.ulidGenerator = ulidGenerator;
        this.createCustomerInputValidator = createCustomerInputValidator;
        this.updateCustomerInputValidator = updateCustomerInputValidator;
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
    public CustomerView createCustomer(final CreateCustomerInput input) {
        createCustomerInputValidator.validate(input);

        final CustomerProfile profile = buildCustomerProfile(input);

        final Customer customer = Customer.createNew(
                CustomerId.newId(ulidGenerator),
                profile,
                new Email(input.email()),
                TEMPORARY_PASSWORD_HASH);

        final Customer savedCustomer = customerService.registerCustomer(customer, input.termsVersion());
        final IssuedActivationToken issuedActivationToken = activationTokenService.createFor(savedCustomer.getId());

        activationEmailService.sendActivationToken(savedCustomer.getEmail(), issuedActivationToken.rawToken());

        return toCustomerView(savedCustomer);
    }

    @Override
    public CustomerView getCustomer(final String customerId) {
        return customerService.getCustomerById(customerId)
                .map(this::toCustomerView)
                .orElseThrow( () -> new CustomerNotFoundException(customerId));
    }

    @Override
    public CustomerView updateCustomer(String customerId, UpdateCustomerInput input) {

        updateCustomerInputValidator.validate(input);

        CustomerProfile profile = buildCustomerProfile(input);
        Customer savedCustomer = customerService.updateCustomer(customerId, profile);

        return toCustomerView(savedCustomer);
    }

    @Override
    public CustomerView activateCustomer(final String customerId, final String rawToken) {
        final Customer customer = customerService.activateCustomer(customerId, rawToken);
        return toCustomerView(customer);
    }

    @Override
    public void resendActivationToken(final String customerId) {
        final ActivationEmailDelivery activationEmailDelivery = customerService.issueActivationTokenForResend(customerId);
        activationEmailService.sendActivationToken(activationEmailDelivery.email(), activationEmailDelivery.rawToken());
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

    private CustomerProfile buildCustomerProfile(final UpdateCustomerInput input) {
        return CustomerProfile.of(
                input.firstName(),
                input.lastName(),
                input.phoneNumber());
    }

    private CustomerProfile buildCustomerProfile(final CreateCustomerInput input) {
        return CustomerProfile.of(
                input.firstName(),
                input.lastName(),
                input.phoneNumber());
    }
}
