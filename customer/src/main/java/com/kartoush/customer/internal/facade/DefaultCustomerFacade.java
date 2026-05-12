package com.kartoush.customer.internal.facade;

import com.kartoush.auth.domain.IssuedPasswordSetupToken;
import com.kartoush.auth.facade.CustomerPasswordFacade;
import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.exception.InvalidPasswordSetupException;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.customer.facade.model.CustomerActivationView;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import com.kartoush.customer.facade.model.UpdateCustomerInput;
import com.kartoush.customer.internal.validation.CustomerPasswordSetupValidator;
import com.kartoush.customer.internal.validation.CustomerRegistrationValidator;
import com.kartoush.customer.internal.validation.CustomerUpdateValidator;
import com.kartoush.customer.service.ActivationEmailDelivery;
import com.kartoush.customer.service.ActivationTokenService;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.customer.service.IssuedActivationToken;
import com.kartoush.notification.email.customer.CustomerEmailFactory;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
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

    private final EmailDeliveryService emailDeliveryService;

    private final CustomerEmailFactory customerEmailFactory;

    private final ActivationTokenService activationTokenService;

    private final CustomerPasswordFacade customerPasswordFacade;

    private final UlidGenerator ulidGenerator;

    private final CustomerPasswordSetupValidator customerPasswordSetupValidator;

    private final CustomerUpdateValidator customerUpdateValidator;

    private final CustomerRegistrationValidator customerRegistrationValidator;

    public DefaultCustomerFacade(
        final CustomerService customerService,
        final EmailDeliveryService emailDeliveryService,
        final CustomerEmailFactory customerEmailFactory,
        final ActivationTokenService activationTokenService,
        final CustomerPasswordFacade customerPasswordFacade,
        final UlidGenerator ulidGenerator,
        final CustomerPasswordSetupValidator customerPasswordSetupValidator,
        final CustomerUpdateValidator customerUpdateValidator, CustomerRegistrationValidator customerRegistrationValidator) {
        this.customerService = customerService;
        this.emailDeliveryService = emailDeliveryService;
        this.customerEmailFactory = customerEmailFactory;
        this.activationTokenService = activationTokenService;
        this.customerPasswordFacade = customerPasswordFacade;
        this.ulidGenerator = ulidGenerator;
        this.customerPasswordSetupValidator = customerPasswordSetupValidator;
        this.customerUpdateValidator = customerUpdateValidator;
        this.customerRegistrationValidator = customerRegistrationValidator;
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
        customerRegistrationValidator.validate(input);

        final CustomerProfile profile = buildCustomerProfile(input);

        final Customer customer = Customer.createNew(
            CustomerId.newId(ulidGenerator),
            profile,
            new Email(input.email()));

        final Customer savedCustomer = customerService.registerCustomer(customer, input.termsVersion());
        final IssuedActivationToken issuedActivationToken = activationTokenService.createFor(savedCustomer.getId());

        emailDeliveryService.send(
            customerEmailFactory.newActivationEmail(
                savedCustomer.getEmail(),
                savedCustomer.getId(),
                issuedActivationToken.rawToken()
            )
        );

        return toCustomerView(savedCustomer);
    }

    @Override
    public CustomerView getCustomer(final String customerId) {
        return customerService.getCustomerById(customerId)
            .map(this::toCustomerView)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    @Override
    public CustomerView updateCustomer(String customerId, UpdateCustomerInput input) {

        customerUpdateValidator.validate(input);

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
        customerPasswordSetupValidator.validate(input);

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
        emailDeliveryService.send(
            customerEmailFactory.newActivationEmail(
                activationEmailDelivery.email(),
                activationEmailDelivery.customerId(),
                activationEmailDelivery.rawToken()
            )
        );
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
