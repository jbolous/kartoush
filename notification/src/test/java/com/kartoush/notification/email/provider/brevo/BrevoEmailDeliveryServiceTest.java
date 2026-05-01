package com.kartoush.notification.email.provider.brevo;

import com.kartoush.notification.email.client.EmailApiClient;
import com.kartoush.notification.email.delivery.EmailDeliveryException;
import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.EmailMessageType;
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
        final EmailApiClient emailApiClient = mock(EmailApiClient.class);
        final EmailMessage email = new EmailMessage(
            EmailMessageType.CUSTOMER_PASSWORD_RESET,
            new Email("jack@kartoush.com"),
            new Email("no-reply@notify.kartoush.com"),
            "Kartoush",
            "Reset your Kartoush password",
            "Reset it here",
            "https://kartoush.dev/reset-password?token=abc"
        );
        when(emailApiClient.send(email)).thenReturn(Optional.of("brevo-message-id"));

        final BrevoEmailDeliveryService service = new BrevoEmailDeliveryService(emailApiClient);

        assertThatCode(() -> service.send(email)).doesNotThrowAnyException();
        verify(emailApiClient).send(email);
    }

    @Test
    void shouldRethrowBrevoDeliveryFailures() {
        final EmailApiClient emailApiClient = mock(EmailApiClient.class);
        final EmailMessage email = new EmailMessage(
            EmailMessageType.CUSTOMER_PASSWORD_RESET,
            new Email("jack@kartoush.com"),
            new Email("no-reply@notify.kartoush.com"),
            "Kartoush",
            "Reset your Kartoush password",
            "Reset it here",
            "https://kartoush.dev/reset-password?token=abc"
        );
        final EmailDeliveryException exception = new EmailDeliveryException("brevo", "Brevo email delivery failed");
        doThrow(exception).when(emailApiClient).send(email);

        final BrevoEmailDeliveryService service = new BrevoEmailDeliveryService(emailApiClient);

        assertThatThrownBy(() -> service.send(email))
            .isInstanceOf(EmailDeliveryException.class)
            .hasMessage("Brevo email delivery failed");
    }
}
