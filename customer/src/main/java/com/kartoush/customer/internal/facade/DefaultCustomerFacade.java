package com.kartoush.customer.internal.facade;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CreateCustomerCommand;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.UpdateCustomerCommand;
import com.kartoush.customer.internal.validation.CreateCustomerCommandValidator;
import com.kartoush.customer.internal.validation.UpdateCustomerCommandValidator;
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
    private final CreateCustomerCommandValidator createCustomerCommandValidator;
    private final UpdateCustomerCommandValidator updateCustomerCommandValidator;

    public DefaultCustomerFacade(
            final CustomerService customerService,
            final ActivationEmailService activationEmailService,
            final ActivationTokenService activationTokenService,
            final UlidGenerator ulidGenerator,
            final CreateCustomerCommandValidator createCustomerCommandValidator,
            final UpdateCustomerCommandValidator updateCustomerCommandValidator) {
        this.customerService = customerService;
        this.activationEmailService = activationEmailService;
        this.activationTokenService = activationTokenService;
        this.ulidGenerator = ulidGenerator;
        this.createCustomerCommandValidator = createCustomerCommandValidator;
        this.updateCustomerCommandValidator = updateCustomerCommandValidator;
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
    public CustomerView createCustomer(final CreateCustomerCommand command) {
        createCustomerCommandValidator.validate(command);

        final CustomerProfile profile = buildCustomerProfile(command);

        final Customer customer = Customer.createNew(
                CustomerId.newId(ulidGenerator),
                profile,
                new Email(command.email()),
                TEMPORARY_PASSWORD_HASH);

        final Customer savedCustomer = customerService.registerCustomer(customer, command.termsVersion());
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
    public CustomerView updateCustomer(String customerId, UpdateCustomerCommand command) {

        updateCustomerCommandValidator.validate(command);

        CustomerProfile profile = buildCustomerProfile(command);
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

    private CustomerProfile buildCustomerProfile(final UpdateCustomerCommand command) {
        return CustomerProfile.of(
                command.firstName(),
                command.lastName(),
                command.phoneNumber());
    }

    private CustomerProfile buildCustomerProfile(final CreateCustomerCommand command) {
        return CustomerProfile.of(
                command.firstName(),
                command.lastName(),
                command.phoneNumber());
    }
}
