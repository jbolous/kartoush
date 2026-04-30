package com.kartoush.auth.internal.facade;

import com.kartoush.auth.domain.IssuedCustomerAccessToken;
import com.kartoush.auth.exception.InvalidCustomerCredentialsException;
import com.kartoush.auth.service.CustomerAuthSessionService;
import com.kartoush.auth.service.CustomerPasswordService;
import com.kartoush.platform.types.CustomerId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCustomerSignInFacadeTest {

    private static final CustomerId CUSTOMER_ID =
        CustomerId.of("01J2Z5Y6K4Z6D5H2X3JH8M9N0P");

    @Mock
    private CustomerPasswordService customerPasswordService;

    @Mock
    private CustomerAuthSessionService customerAuthSessionService;

    @InjectMocks
    private DefaultCustomerSignInFacade customerSignInFacade;

    @Test
    void shouldSignInWhenPasswordVerificationSucceeds() {
        final IssuedCustomerAccessToken accessToken =
            new IssuedCustomerAccessToken("opaque-token", "Bearer");

        when(customerPasswordService.verify(CUSTOMER_ID, "Password123!")).thenReturn(true);
        when(customerAuthSessionService.issueFor(CUSTOMER_ID)).thenReturn(accessToken);

        final IssuedCustomerAccessToken result =
            customerSignInFacade.signIn(CUSTOMER_ID, "Password123!");

        assertThat(result).isEqualTo(accessToken);
        verify(customerPasswordService).verify(CUSTOMER_ID, "Password123!");
        verify(customerAuthSessionService).issueFor(CUSTOMER_ID);
    }

    @Test
    void shouldRejectSignInWhenPasswordVerificationFails() {
        when(customerPasswordService.verify(CUSTOMER_ID, "Password123!")).thenReturn(false);

        assertThatThrownBy(() -> customerSignInFacade.signIn(CUSTOMER_ID, "Password123!"))
            .isInstanceOf(InvalidCustomerCredentialsException.class);
    }
}
