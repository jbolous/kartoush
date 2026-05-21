package com.kartoush.customer.internal.facade;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.facade.model.AuthCandidateView;
import com.kartoush.customer.service.CustomerService;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCustomerAuthenticationFacadeTest {

    private static final String CUSTOMER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "+13125550100";
    private static final Email EMAIL = new Email("jack@kartoush.com");

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private DefaultCustomerAuthenticationFacade customerAuthenticationFacade;

    @Test
    void shouldReturnAuthenticationCandidateWhenCustomerExists() {
        final Customer customer = Customer.fromPersistence(
            CustomerId.of(CUSTOMER_ID),
            CustomerProfile.of(FIRST_NAME, LAST_NAME, PHONE_NUMBER),
            EMAIL,
            CustomerStatus.ACTIVE,
            java.util.List.of()
        );

        when(customerService.findCustomerByEmail(EMAIL)).thenReturn(Optional.of(customer));

        final Optional<AuthCandidateView> result =
            customerAuthenticationFacade.findAuthCandidateByEmail(EMAIL);

        assertThat(result).contains(new AuthCandidateView(
            CUSTOMER_ID,
            EMAIL.value(),
            CustomerStatus.ACTIVE
        ));
        verify(customerService).findCustomerByEmail(EMAIL);
    }

    @Test
    void shouldReturnEmptyWhenCustomerDoesNotExist() {
        when(customerService.findCustomerByEmail(EMAIL)).thenReturn(Optional.empty());

        final Optional<AuthCandidateView> result =
            customerAuthenticationFacade.findAuthCandidateByEmail(EMAIL);

        assertThat(result).isEmpty();
        verify(customerService).findCustomerByEmail(EMAIL);
    }

    @Test
    void shouldReturnAuthenticationCandidateWhenCustomerExistsById() {
        final Customer customer = Customer.fromPersistence(
            CustomerId.of(CUSTOMER_ID),
            CustomerProfile.of(FIRST_NAME, LAST_NAME, PHONE_NUMBER),
            EMAIL,
            CustomerStatus.ACTIVE,
            java.util.List.of()
        );

        when(customerService.getCustomerById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        final Optional<AuthCandidateView> result =
            customerAuthenticationFacade.findAuthCandidateById(CUSTOMER_ID);

        assertThat(result).contains(new AuthCandidateView(
            CUSTOMER_ID,
            EMAIL.value(),
            CustomerStatus.ACTIVE
        ));
        verify(customerService).getCustomerById(CUSTOMER_ID);
    }

    @Test
    void shouldReturnEmptyWhenCustomerDoesNotExistById() {
        when(customerService.getCustomerById(CUSTOMER_ID)).thenReturn(Optional.empty());

        final Optional<AuthCandidateView> result =
            customerAuthenticationFacade.findAuthCandidateById(CUSTOMER_ID);

        assertThat(result).isEmpty();
        verify(customerService).getCustomerById(CUSTOMER_ID);
    }
}
