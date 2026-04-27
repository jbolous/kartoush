package com.kartoush.auth.internal.facade;

import com.kartoush.auth.domain.CustomerCredential;
import com.kartoush.auth.service.CustomerCredentialService;
import com.kartoush.platform.types.CustomerId;
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
class DefaultCustomerCredentialFacadeTest {

    private static final CustomerId CUSTOMER_ID =
        CustomerId.of("01J2Z5Y6K4Z6D5H2X3JH8M9N0P");
    private static final String PASSWORD_HASH = "hash";

    @Mock
    private CustomerCredentialService customerCredentialService;

    @InjectMocks
    private DefaultCustomerCredentialFacade customerCredentialFacade;

    @Test
    void shouldDelegateCredentialLookup() {
        final CustomerCredential credential =
            new CustomerCredential(CUSTOMER_ID, PASSWORD_HASH);

        when(customerCredentialService.findByCustomerId(CUSTOMER_ID))
            .thenReturn(Optional.of(credential));

        final Optional<CustomerCredential> result =
            customerCredentialFacade.findByCustomerId(CUSTOMER_ID);

        assertThat(result).contains(credential);
        verify(customerCredentialService).findByCustomerId(CUSTOMER_ID);
    }

    @Test
    void shouldDelegateInitialCredentialCreation() {
        final CustomerCredential credential =
            new CustomerCredential(CUSTOMER_ID, PASSWORD_HASH);

        when(customerCredentialService.createInitialCredential(CUSTOMER_ID, PASSWORD_HASH))
            .thenReturn(credential);

        final CustomerCredential result =
            customerCredentialFacade.createInitialCredential(CUSTOMER_ID, PASSWORD_HASH);

        assertThat(result).isEqualTo(credential);
        verify(customerCredentialService).createInitialCredential(CUSTOMER_ID, PASSWORD_HASH);
    }
}
