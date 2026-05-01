package com.kartoush.auth.email;

import com.kartoush.platform.types.Email;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrevoEmailDeliveryServiceTest {

    @Test
    void shouldDelegateToBrevoApiClient() {
        final BrevoEmailApiClient brevoEmailApiClient = mock(BrevoEmailApiClient.class);
        final EmailMessage email = new EmailMessage(
            EmailMessageType.CUSTOMER_PASSWORD_RESET,
            new Email("jack@kartoush.com"),
            new Email("no-reply@notify.kartoush.com"),
            "Kartoush",
            "Reset your Kartoush password",
            "Reset it here",
            "https://kartoush.dev/reset-password?token=abc"
        );
        when(brevoEmailApiClient.send(email)).thenReturn(Optional.of("brevo-message-id"));

        final BrevoEmailDeliveryService service = new BrevoEmailDeliveryService(brevoEmailApiClient);

        assertThatCode(() -> service.send(email)).doesNotThrowAnyException();
        verify(brevoEmailApiClient).send(email);
    }

    @Test
    void shouldRethrowBrevoDeliveryFailures() {
        final BrevoEmailApiClient brevoEmailApiClient = mock(BrevoEmailApiClient.class);
        final EmailMessage email = new EmailMessage(
            EmailMessageType.CUSTOMER_PASSWORD_RESET,
            new Email("jack@kartoush.com"),
            new Email("no-reply@notify.kartoush.com"),
            "Kartoush",
            "Reset your Kartoush password",
            "Reset it here",
            "https://kartoush.dev/reset-password?token=abc"
        );
        final EmailDeliveryException exception = new EmailDeliveryException("Brevo email delivery failed");
        doThrow(exception).when(brevoEmailApiClient).send(email);

        final BrevoEmailDeliveryService service = new BrevoEmailDeliveryService(brevoEmailApiClient);

        assertThatThrownBy(() -> service.send(email))
            .isInstanceOf(EmailDeliveryException.class)
            .hasMessage("Brevo email delivery failed");
    }
}
