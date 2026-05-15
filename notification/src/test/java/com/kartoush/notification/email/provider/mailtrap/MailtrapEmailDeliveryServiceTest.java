package com.kartoush.notification.email.provider.mailtrap;

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

class MailtrapEmailDeliveryServiceTest {

    @Test
    void shouldDelegateToMailtrapEmailClient() {
        final EmailClient emailClient = mock(EmailClient.class);
        final EmailMessage email = new EmailMessage(
            EmailMessageType.CUSTOMER_ACTIVATION,
            new Email("jack@kartoush.com"),
            new Email("no-reply@notify.kartoush.com"),
            "Kartoush",
            "Activate your Kartoush account",
            "Hello from Kartoush",
            "https://kartoush.dev/activate?token=abc"
        );
        when(emailClient.send(email)).thenReturn(Optional.of("mailtrap-message-id"));

        final MailtrapEmailDeliveryService service = new MailtrapEmailDeliveryService(emailClient);

        assertThatCode(() -> service.send(email)).doesNotThrowAnyException();
        verify(emailClient).send(email);
    }

    @Test
    void shouldRethrowMailtrapEmailDeliveryFailures() {
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
        final EmailDeliveryException exception = new EmailDeliveryException("mailtrap", "Mailtrap email delivery failed");
        doThrow(exception).when(emailClient).send(email);

        final MailtrapEmailDeliveryService service = new MailtrapEmailDeliveryService(emailClient);

        assertThatThrownBy(() -> service.send(email))
            .isInstanceOf(EmailDeliveryException.class)
            .hasMessage("Mailtrap email delivery failed");
    }
}
