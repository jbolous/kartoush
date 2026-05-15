package com.kartoush.notification.email.provider.brevo;

import com.kartoush.notification.email.client.EmailClient;
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
    void shouldDelegateToBrevoEmailClient() {
        final EmailClient emailClient = mock(EmailClient.class);
        final EmailMessage email = new EmailMessage(
            EmailMessageType.CUSTOMER_PASSWORD_RESET,
            new Email("jack@kartoush.com"),
            new Email("no-reply@notify.kartoush.com"),
            "Kartoush",
            "Reset your Kartoush password",
            "Reset it here",
            "https://kartoush.dev/reset-password?token=abc"
        );
        when(emailClient.send(email)).thenReturn(Optional.of("brevo-message-id"));

        final BrevoEmailDeliveryService service = new BrevoEmailDeliveryService(emailClient);

        assertThatCode(() -> service.send(email)).doesNotThrowAnyException();
        verify(emailClient).send(email);
    }

    @Test
    void shouldRethrowBrevoEmailDeliveryFailures() {
        final EmailClient emailClient = mock(EmailClient.class);
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
        doThrow(exception).when(emailClient).send(email);

        final BrevoEmailDeliveryService service = new BrevoEmailDeliveryService(emailClient);

        assertThatThrownBy(() -> service.send(email))
            .isInstanceOf(EmailDeliveryException.class)
            .hasMessage("Brevo email delivery failed");
    }
}
