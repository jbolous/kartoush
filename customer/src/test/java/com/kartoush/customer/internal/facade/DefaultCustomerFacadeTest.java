package com.kartoush.customer.internal.facade;

import com.kartoush.auth.domain.PasswordSetupToken;
import com.kartoush.auth.domain.IssuedPasswordSetupToken;
import com.kartoush.notification.email.customer.CustomerEmailFactory;
import com.kartoush.notification.email.delivery.EmailDeliveryService;
import com.kartoush.notification.email.EmailMessage;
import com.kartoush.auth.facade.CustomerPasswordFacade;
import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.customer.facade.model.CustomerActivationView;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import com.kartoush.customer.internal.validation.CreateCustomerInputValidator;
import com.kartoush.customer.internal.validation.SetInitialCustomerPasswordInputValidator;
import com.kartoush.customer.service.ActivationEmailDelivery;
import com.kartoush.customer.service.ActivationTokenService;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.customer.service.IssuedActivationToken;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.ulid.UlidGenerator;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCustomerFacadeTest {

    private static final String CUSTOMER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String EMAIL = "jack@kartoush.com";
    private static final String RAW_TOKEN = "raw-token";
    private static final String PHONE_NUMBER = "312-555-0100";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String TERMS_VERSION = "2026.04.01";
    private static final String SETUP_TOKEN = "setup-token";

    @Mock
    private UlidGenerator ulidGenerator;

    @Mock
    private CreateCustomerInputValidator validator;

    @Mock
    private SetInitialCustomerPasswordInputValidator setInitialCustomerPasswordInputValidator;

    @Mock
    private CustomerService customerService;

    @Mock
    private EmailDeliveryService emailDeliveryService;

    @Mock
    private CustomerEmailFactory customerEmailFactory;

    @Mock
    private ActivationTokenService activationTokenService;

    @Mock
    private CustomerPasswordFacade customerPasswordFacade;

    @InjectMocks
    private DefaultCustomerFacade facade;

    @Test
    void shouldCreateCustomer() {
        // given
        final var request = buildCustomerRequest();
        final var saved = buildCustomer();
        final var view = buildCustomerView();
        final var issuedActivationToken = new IssuedActivationToken(mock(com.kartoush.customer.domain.ActivationToken.class), RAW_TOKEN);
        final EmailMessage activationEmail = mock(EmailMessage.class);

        // when
        when(ulidGenerator.next()).thenReturn(CUSTOMER_ID);
        when(customerService.registerCustomer(any(), any())).thenReturn(saved);
        when(activationTokenService.createFor(CustomerId.of(CUSTOMER_ID))).thenReturn(issuedActivationToken);
        when(customerEmailFactory.newActivationEmail(new Email(EMAIL), CustomerId.of(CUSTOMER_ID), RAW_TOKEN))
            .thenReturn(activationEmail);

        final var result = facade.createCustomer(request);

        // then
        assertThat(result).isEqualTo(view);
        verify(validator).validate(request);
        verify(customerService).registerCustomer(any(), org.mockito.ArgumentMatchers.eq(TERMS_VERSION));
        verify(activationTokenService).createFor(CustomerId.of(CUSTOMER_ID));
        verify(emailDeliveryService).send(activationEmail);
    }

    @Test
    void shouldActivateCustomerByToken() {
        // given
        final Customer activatedCustomer = buildActivatedCustomer();
        final CustomerActivationView activatedView = buildActivatedCustomerView();
        final IssuedPasswordSetupToken issuedSetupToken =
            new IssuedPasswordSetupToken(
                new PasswordSetupToken(
                    "01JSETUPTOKENID0000000000000",
                    CustomerId.of(CUSTOMER_ID),
                    "setup-token-hash",
                    Instant.parse("2026-04-28T00:00:00Z"),
                    null,
                    Instant.parse("2026-04-27T00:00:00Z")
                ),
                SETUP_TOKEN
            );

        when(customerService.activateCustomer(CUSTOMER_ID, RAW_TOKEN)).thenReturn(activatedCustomer);
        when(customerPasswordFacade.issuePasswordSetupToken(CustomerId.of(CUSTOMER_ID)))
            .thenReturn(issuedSetupToken);

        // when
        final CustomerActivationView result = facade.activateCustomer(CUSTOMER_ID, RAW_TOKEN);

        // then
        assertThat(result).isEqualTo(activatedView);
        verify(customerService).activateCustomer(CUSTOMER_ID, RAW_TOKEN);
        verify(customerPasswordFacade).issuePasswordSetupToken(CustomerId.of(CUSTOMER_ID));
    }

    @Test
    void shouldSetInitialPasswordForActiveCustomer() {
        final InitialCustomerPasswordInput input =
            new InitialCustomerPasswordInput(SETUP_TOKEN, "Password123!", "Password123!");
        final Customer activeCustomer = buildActivatedCustomer();

        when(customerService.getCustomerById(CUSTOMER_ID)).thenReturn(Optional.of(activeCustomer));

        facade.setInitialPassword(CUSTOMER_ID, input);

        verify(setInitialCustomerPasswordInputValidator).validate(input);
        verify(customerPasswordFacade).setInitialPassword(
            CustomerId.of(CUSTOMER_ID),
            SETUP_TOKEN,
            "Password123!"
        );
    }

    @Test
    void shouldResendActivationToken() {
        // given
        final ActivationEmailDelivery activationEmail =
            new ActivationEmailDelivery(CustomerId.of(CUSTOMER_ID), new Email(EMAIL), RAW_TOKEN);
        final EmailMessage email = mock(EmailMessage.class);
        when(customerService.issueActivationTokenForResend(CUSTOMER_ID)).thenReturn(activationEmail);
        when(customerEmailFactory.newActivationEmail(new Email(EMAIL), CustomerId.of(CUSTOMER_ID), RAW_TOKEN))
            .thenReturn(email);

        // when
        facade.resendActivationToken(CUSTOMER_ID);

        // then
        verify(customerService).issueActivationTokenForResend(CUSTOMER_ID);
        verify(emailDeliveryService).send(email);
    }

    private CreateCustomerInput buildCustomerRequest(){
        return  new CreateCustomerInput(
                FIRST_NAME,
                LAST_NAME,
                EMAIL,
                PHONE_NUMBER,
                true,
                TERMS_VERSION);
    }

    private Customer buildCustomer(){

        CustomerId customerId = CustomerId.of(CUSTOMER_ID);
        CustomerProfile profile = CustomerProfile.of(FIRST_NAME, LAST_NAME, PHONE_NUMBER);
        return Customer.createNew(
                customerId,
                profile,
                new Email(EMAIL));
    }

    private CustomerView buildCustomerView(){
        return new CustomerView(
                                CUSTOMER_ID,
                                FIRST_NAME,
                                LAST_NAME,
                                EMAIL,
                                PHONE_NUMBER,
                                CustomerStatus.PENDING);
    }

    private Customer buildActivatedCustomer() {
        final CustomerId customerId = CustomerId.of(CUSTOMER_ID);
        final CustomerProfile profile = CustomerProfile.of(FIRST_NAME, LAST_NAME, PHONE_NUMBER);
        final Customer customer = Customer.createNew(
            customerId,
            profile,
            new Email(EMAIL));
        customer.activate();
        return customer;
    }

    private CustomerActivationView buildActivatedCustomerView() {
        return new CustomerActivationView(
            CUSTOMER_ID,
            FIRST_NAME,
            LAST_NAME,
            EMAIL,
            PHONE_NUMBER,
            CustomerStatus.ACTIVE,
            SETUP_TOKEN);
    }
}
