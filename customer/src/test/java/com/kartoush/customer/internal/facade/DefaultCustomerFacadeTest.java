package com.kartoush.customer.internal.facade;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.internal.validation.CreateCustomerRequestValidator;
import com.kartoush.customer.service.ActivationEmailDelivery;
import com.kartoush.customer.service.ActivationEmailService;
import com.kartoush.customer.service.ActivationTokenService;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.customer.service.IssuedActivationToken;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.ulid.UlidGenerator;
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
    private static final String PASSWORD = "password";
    private static final String RAW_TOKEN = "raw-token";
    private static final String PHONE_NUMBER = "312-555-0100";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String TERMS_VERSION = "2026-04";

    @Mock
    private UlidGenerator ulidGenerator;

    @Mock
    private CreateCustomerRequestValidator validator;

    @Mock
    private CustomerService customerService;

    @Mock
    private ActivationEmailService activationEmailService;

    @Mock
    private ActivationTokenService activationTokenService;

    @InjectMocks
    private DefaultCustomerFacade facade;

    @Test
    void shouldCreateCustomer() {
        // given
        final var request = buildCustomerRequest();
        final var saved = buildCustomer();
        final var view = buildCustomerView();
        final var issuedActivationToken = new IssuedActivationToken(mock(com.kartoush.customer.domain.ActivationToken.class), RAW_TOKEN);

        // when
        when(ulidGenerator.next()).thenReturn(CUSTOMER_ID);
        when(customerService.createCustomer(any())).thenReturn(saved);
        when(activationTokenService.createFor(CustomerId.of(CUSTOMER_ID))).thenReturn(issuedActivationToken);

        final var result = facade.createCustomer(request);

        // then
        assertThat(result).isEqualTo(view);
        verify(validator).validate(request);
        verify(customerService).createCustomer(any());
        verify(activationTokenService).createFor(CustomerId.of(CUSTOMER_ID));
        verify(activationEmailService).sendActivationToken(new Email(EMAIL), RAW_TOKEN);
    }

    @Test
    void shouldActivateCustomerByToken() {
        // given
        final Customer activatedCustomer = buildActivatedCustomer();
        final CustomerView activatedView = buildActivatedCustomerView();

        when(customerService.activateCustomer(CUSTOMER_ID, RAW_TOKEN)).thenReturn(activatedCustomer);

        // when
        final CustomerView result = facade.activateCustomer(CUSTOMER_ID, RAW_TOKEN);

        // then
        assertThat(result).isEqualTo(activatedView);
        verify(customerService).activateCustomer(CUSTOMER_ID, RAW_TOKEN);
    }

    @Test
    void shouldResendActivationToken() {
        // given
        final ActivationEmailDelivery activationEmail =
            new ActivationEmailDelivery(new Email(EMAIL), RAW_TOKEN);
        when(customerService.issueActivationTokenForResend(CUSTOMER_ID)).thenReturn(activationEmail);

        // when
        facade.resendActivationToken(CUSTOMER_ID);

        // then
        verify(customerService).issueActivationTokenForResend(CUSTOMER_ID);
        verify(activationEmailService).sendActivationToken(new Email(EMAIL), RAW_TOKEN);
    }

    private CreateCustomerRequest buildCustomerRequest(){
        return  new CreateCustomerRequest(
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
                new Email(EMAIL),
                PASSWORD);
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
            new Email(EMAIL),
            PASSWORD);
        customer.activate();
        return customer;
    }

    private CustomerView buildActivatedCustomerView() {
        return new CustomerView(
            CUSTOMER_ID,
            FIRST_NAME,
            LAST_NAME,
            EMAIL,
            PHONE_NUMBER,
            CustomerStatus.ACTIVE);
    }
}
