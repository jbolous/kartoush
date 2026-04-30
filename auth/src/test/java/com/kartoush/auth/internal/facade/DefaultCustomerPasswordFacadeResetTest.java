package com.kartoush.auth.internal.facade;

import com.kartoush.auth.domain.CustomerPassword;
import com.kartoush.auth.domain.IssuedPasswordResetToken;
import com.kartoush.auth.domain.PasswordResetToken;
import com.kartoush.auth.exception.PasswordReuseNotAllowedException;
import com.kartoush.auth.service.CustomerAuthSessionService;
import com.kartoush.auth.service.CustomerPasswordService;
import com.kartoush.auth.service.PasswordResetTokenService;
import com.kartoush.platform.types.CustomerId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCustomerPasswordFacadeResetTest {

    private static final CustomerId CUSTOMER_ID =
        CustomerId.of("01J2Z5Y6K4Z6D5H2X3JH8M9N0P");
    private static final String PASSWORD_HASH = "hash";
    private static final String RAW_RESET_TOKEN = "reset-token";
    private static final String RAW_PASSWORD = "Password123!";
    private static final String NEW_PASSWORD = "BetterPassword123!";

    @Mock
    private CustomerPasswordService customerPasswordService;

    @Mock
    private CustomerAuthSessionService customerAuthSessionService;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @InjectMocks
    private DefaultCustomerPasswordFacade customerPasswordFacade;

    @Test
    void shouldDelegatePasswordResetTokenIssuance() {
        final IssuedPasswordResetToken issuedToken =
            new IssuedPasswordResetToken(
                new PasswordResetToken(
                    "01JRESETTOKENID00000000000",
                    CUSTOMER_ID,
                    "reset-token-hash",
                    Instant.parse("2026-04-28T00:00:00Z"),
                    null,
                    Instant.parse("2026-04-27T00:00:00Z")
                ),
                RAW_RESET_TOKEN
            );

        when(passwordResetTokenService.issuePasswordResetTokenFor(CUSTOMER_ID)).thenReturn(issuedToken);

        final IssuedPasswordResetToken result = customerPasswordFacade.issuePasswordResetToken(CUSTOMER_ID);

        assertThat(result).isEqualTo(issuedToken);
        verify(passwordResetTokenService).issuePasswordResetTokenFor(CUSTOMER_ID);
    }

    @Test
    void shouldResetPasswordWhenTokenIsValidAndPasswordChanges() {
        final CustomerPassword currentPassword = new CustomerPassword(CUSTOMER_ID, PASSWORD_HASH);
        final CustomerPassword resetPassword = new CustomerPassword(CUSTOMER_ID, "new-hash");
        final PasswordResetToken resetToken =
            new PasswordResetToken(
                "01JRESETTOKENID00000000000",
                CUSTOMER_ID,
                "reset-token-hash",
                Instant.parse("2026-04-28T00:00:00Z"),
                null,
                Instant.parse("2026-04-27T00:00:00Z")
            );

        when(passwordResetTokenService.validate(CUSTOMER_ID, RAW_RESET_TOKEN)).thenReturn(resetToken);
        when(customerPasswordService.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(currentPassword));
        when(customerPasswordService.verify(CUSTOMER_ID, NEW_PASSWORD)).thenReturn(false);
        when(customerPasswordService.resetPassword(CUSTOMER_ID, NEW_PASSWORD)).thenReturn(resetPassword);

        final CustomerPassword result =
            customerPasswordFacade.resetPassword(CUSTOMER_ID, RAW_RESET_TOKEN, NEW_PASSWORD);

        assertThat(result).isEqualTo(resetPassword);
        verify(passwordResetTokenService).validate(CUSTOMER_ID, RAW_RESET_TOKEN);
        verify(customerPasswordService).resetPassword(CUSTOMER_ID, NEW_PASSWORD);
        verify(customerAuthSessionService).revokeAllFor(CUSTOMER_ID);
        verify(passwordResetTokenService).consume(resetToken);
    }

    @Test
    void shouldRejectPasswordResetWhenNewPasswordMatchesCurrentPassword() {
        final CustomerPassword currentPassword = new CustomerPassword(CUSTOMER_ID, PASSWORD_HASH);
        final PasswordResetToken resetToken =
            new PasswordResetToken(
                "01JRESETTOKENID00000000000",
                CUSTOMER_ID,
                "reset-token-hash",
                Instant.parse("2026-04-28T00:00:00Z"),
                null,
                Instant.parse("2026-04-27T00:00:00Z")
            );

        when(passwordResetTokenService.validate(CUSTOMER_ID, RAW_RESET_TOKEN)).thenReturn(resetToken);
        when(customerPasswordService.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(currentPassword));
        when(customerPasswordService.verify(CUSTOMER_ID, RAW_PASSWORD)).thenReturn(true);

        assertThatThrownBy(() ->
            customerPasswordFacade.resetPassword(CUSTOMER_ID, RAW_RESET_TOKEN, RAW_PASSWORD))
            .isInstanceOf(PasswordReuseNotAllowedException.class)
            .hasMessageContaining(CUSTOMER_ID.value());
    }
}
