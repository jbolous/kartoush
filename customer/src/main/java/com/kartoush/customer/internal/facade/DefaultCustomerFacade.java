package com.kartoush.customer.internal.facade;

import com.kartoush.auth.domain.IssuedPasswordSetupToken;
import com.kartoush.auth.facade.CustomerPasswordFacade;
import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.exception.InvalidPasswordSetupException;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.customer.facade.model.CustomerActivationView;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import com.kartoush.customer.facade.model.UpdateCustomerInput;
import com.kartoush.customer.internal.validation.CreateCustomerInputValidator;
import com.kartoush.customer.internal.validation.SetInitialCustomerPasswordInputValidator;
import com.kartoush.customer.internal.validation.UpdateCustomerInputValidator;
import com.kartoush.customer.service.ActivationEmailDelivery;
import com.kartoush.customer.service.ActivationEmailService;
import com.kartoush.customer.service.ActivationTokenService;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.customer.service.IssuedActivationToken;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.ulid.UlidGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DefaultCustomerFacade implements CustomerFacade {

    private final CustomerService customerService;
    private final ActivationEmailService activationEmailService;
    private final ActivationTokenService activationTokenService;
    private final CustomerPasswordFacade customerPasswordFacade;
    private final UlidGenerator ulidGenerator;
    private final CreateCustomerInputValidator createCustomerInputValidator;
    private final SetInitialCustomerPasswordInputValidator setInitialCustomerPasswordInputValidator;
    private final UpdateCustomerInputValidator updateCustomerInputValidator;

    public DefaultCustomerFacade(
            final CustomerService customerService,
            final ActivationEmailService activationEmailService,
            final ActivationTokenService activationTokenService,
            final CustomerPasswordFacade customerPasswordFacade,
            final UlidGenerator ulidGenerator,
            final CreateCustomerInputValidator createCustomerInputValidator,
            final SetInitialCustomerPasswordInputValidator setInitialCustomerPasswordInputValidator,
            final UpdateCustomerInputValidator updateCustomerInputValidator) {
        this.customerService = customerService;
        this.activationEmailService = activationEmailService;
        this.activationTokenService = activationTokenService;
        this.customerPasswordFacade = customerPasswordFacade;
        this.ulidGenerator = ulidGenerator;
        this.createCustomerInputValidator = createCustomerInputValidator;
        this.setInitialCustomerPasswordInputValidator = setInitialCustomerPasswordInputValidator;
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
                new Email(input.email()));

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
    @Transactional
    public CustomerActivationView activateCustomer(final String customerId, final String rawToken) {
        final Customer customer = customerService.activateCustomer(customerId, rawToken);
        final IssuedPasswordSetupToken issuedSetupToken =
            customerPasswordFacade.issuePasswordSetupToken(customer.getId());

        return toCustomerActivationView(customer, issuedSetupToken.rawToken());
    }

    @Override
    public void setInitialPassword(final String customerId, final InitialCustomerPasswordInput input) {
        setInitialCustomerPasswordInputValidator.validate(input);

        final Customer customer = customerService.getCustomerById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new InvalidPasswordSetupException(customer.getStatus());
        }

        customerPasswordFacade.setInitialPassword(
            customer.getId(),
            input.setupToken(),
            input.password()
        );
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

    private CustomerActivationView toCustomerActivationView(final Customer customer, final String passwordSetupToken) {
        return new CustomerActivationView(
            customer.getId().value(),
            customer.getProfile().firstName(),
            customer.getProfile().lastName(),
            customer.getEmail().value(),
            customer.getProfile().phoneNumber(),
            customer.getStatus(),
            passwordSetupToken
        );
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
