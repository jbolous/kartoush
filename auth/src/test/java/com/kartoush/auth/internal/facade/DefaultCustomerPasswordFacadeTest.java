package com.kartoush.auth.internal.facade;

import com.kartoush.auth.domain.CustomerPassword;
import com.kartoush.auth.domain.PasswordSetupToken;
import com.kartoush.auth.domain.IssuedPasswordSetupToken;
import com.kartoush.auth.exception.CustomerPasswordAlreadyExistsException;
import com.kartoush.auth.service.PasswordSetupTokenService;
import com.kartoush.auth.service.CustomerPasswordService;
import com.kartoush.platform.types.CustomerId;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCustomerPasswordFacadeTest {

    private static final CustomerId CUSTOMER_ID =
        CustomerId.of("01J2Z5Y6K4Z6D5H2X3JH8M9N0P");
    private static final String PASSWORD_HASH = "hash";
    private static final String RAW_SETUP_TOKEN = "setup-token";
    private static final String RAW_PASSWORD = "Password123!";
    @Mock
    private CustomerPasswordService customerPasswordService;

    @Mock
    private PasswordSetupTokenService passwordSetupTokenService;

    @InjectMocks
    private DefaultCustomerPasswordFacade customerPasswordFacade;

    @Test
    void shouldDelegateCredentialLookup() {
        final CustomerPassword credential =
            new CustomerPassword(CUSTOMER_ID, PASSWORD_HASH);

        when(customerPasswordService.findByCustomerId(CUSTOMER_ID))
            .thenReturn(Optional.of(credential));

        final Optional<CustomerPassword> result =
            customerPasswordFacade.findByCustomerId(CUSTOMER_ID);

        assertThat(result).contains(credential);
        verify(customerPasswordService).findByCustomerId(CUSTOMER_ID);
    }

    @Test
    void shouldDelegatePasswordSetupTokenIssuance() {
        final IssuedPasswordSetupToken issuedToken =
            new IssuedPasswordSetupToken(
                new PasswordSetupToken(
                    "01JSETUPTOKENID0000000000000",
                    CUSTOMER_ID,
                    "setup-token-hash",
                    Instant.parse("2026-04-28T00:00:00Z"),
                    null,
                    Instant.parse("2026-04-27T00:00:00Z")
                ),
                RAW_SETUP_TOKEN
            );

        when(passwordSetupTokenService.issueFor(CUSTOMER_ID)).thenReturn(issuedToken);

        final IssuedPasswordSetupToken result =
            customerPasswordFacade.issuePasswordSetupToken(CUSTOMER_ID);

        assertThat(result).isEqualTo(issuedToken);
        verify(passwordSetupTokenService).issueFor(CUSTOMER_ID);
    }

    @Test
    void shouldDelegateInitialPasswordEstablishment() {
        final CustomerPassword customerPassword =
            new CustomerPassword(CUSTOMER_ID, PASSWORD_HASH);
        final PasswordSetupToken setupToken =
            new PasswordSetupToken(
                "01JSETUPTOKENID0000000000000",
                CUSTOMER_ID,
                "setup-token-hash",
                Instant.parse("2026-04-28T00:00:00Z"),
                null,
                Instant.parse("2026-04-27T00:00:00Z")
            );

        when(customerPasswordService.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());
        when(passwordSetupTokenService.validate(CUSTOMER_ID, RAW_SETUP_TOKEN)).thenReturn(setupToken);
        when(customerPasswordService.setInitialPassword(CUSTOMER_ID, RAW_PASSWORD))
            .thenReturn(customerPassword);

        final CustomerPassword result =
            customerPasswordFacade.setInitialPassword(CUSTOMER_ID, RAW_SETUP_TOKEN, RAW_PASSWORD);

        assertThat(result).isEqualTo(customerPassword);
        verify(passwordSetupTokenService).validate(CUSTOMER_ID, RAW_SETUP_TOKEN);
        verify(customerPasswordService).setInitialPassword(CUSTOMER_ID, RAW_PASSWORD);
        verify(passwordSetupTokenService).consume(setupToken);
    }

    @Test
    void shouldFailWhenPasswordAlreadyExistsEvenIfTokenIsValid() {
        final CustomerPassword customerPassword =
            new CustomerPassword(CUSTOMER_ID, PASSWORD_HASH);
        final PasswordSetupToken setupToken =
            new PasswordSetupToken(
                "01JSETUPTOKENID0000000000000",
                CUSTOMER_ID,
                "setup-token-hash",
                Instant.parse("2026-04-28T00:00:00Z"),
                null,
                Instant.parse("2026-04-27T00:00:00Z")
            );

        when(passwordSetupTokenService.validate(CUSTOMER_ID, RAW_SETUP_TOKEN)).thenReturn(setupToken);
        when(customerPasswordService.findByCustomerId(CUSTOMER_ID))
            .thenReturn(Optional.of(customerPassword));

        assertThatThrownBy(() ->
            customerPasswordFacade.setInitialPassword(CUSTOMER_ID, RAW_SETUP_TOKEN, RAW_PASSWORD))
            .isInstanceOf(CustomerPasswordAlreadyExistsException.class)
            .hasMessageContaining(CUSTOMER_ID.value());

        verify(passwordSetupTokenService).validate(CUSTOMER_ID, RAW_SETUP_TOKEN);
        verify(customerPasswordService).findByCustomerId(CUSTOMER_ID);
    }
}
