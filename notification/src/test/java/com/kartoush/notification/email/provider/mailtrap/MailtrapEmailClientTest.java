package com.kartoush.notification.email.provider.mailtrap;

import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.EmailMessageType;
import com.kartoush.notification.email.client.EmailClient;
import com.kartoush.notification.email.delivery.EmailDeliveryException;
import com.kartoush.platform.types.Email;
import io.mailtrap.client.MailtrapClient;
import io.mailtrap.model.request.emails.MailtrapMail;
import io.mailtrap.model.response.emails.SendResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailtrapEmailClientTest {

    @Test
    void shouldSendEmailAndReturnFirstMessageId() {
        final MailtrapClient mailtrapClient = mock(MailtrapClient.class);
        final SendResponse response = new SendResponse();
        response.setSuccess(true);
        response.setMessageIds(List.of("mailtrap-message-id", "mailtrap-message-id-2"));
        when(mailtrapClient.send(any(MailtrapMail.class))).thenReturn(response);

        final EmailClient client = new MailtrapEmailClient(mailtrapClient);
        final EmailMessage email = new EmailMessage(
            EmailMessageType.CUSTOMER_ACTIVATION,
            new Email("jack@kartoush.com"),
            new Email("no-reply@notify.kartoush.com"),
            "Kartoush",
            "Activate your Kartoush account",
            "Click here",
            "https://kartoush.dev/activate?token=abc",
            "<p><a href=\"https://kartoush.dev/activate?token=abc\">Activate</a></p>"
        );

        final Optional<String> messageId = client.send(email);

        final ArgumentCaptor<MailtrapMail> mailCaptor = ArgumentCaptor.forClass(MailtrapMail.class);
        verify(mailtrapClient).send(mailCaptor.capture());

        assertThat(messageId).contains("mailtrap-message-id");
        assertThat(mailCaptor.getValue().getFrom().getEmail()).isEqualTo("no-reply@notify.kartoush.com");
        assertThat(mailCaptor.getValue().getFrom().getName()).isEqualTo("Kartoush");
        assertThat(mailCaptor.getValue().getTo()).hasSize(1);
        assertThat(mailCaptor.getValue().getTo().getFirst().getEmail()).isEqualTo("jack@kartoush.com");
        assertThat(mailCaptor.getValue().getSubject()).isEqualTo("Activate your Kartoush account");
        assertThat(mailCaptor.getValue().getText()).isEqualTo("Click here");
        assertThat(mailCaptor.getValue().getHtml()).isEqualTo("<p><a href=\"https://kartoush.dev/activate?token=abc\">Activate</a></p>");
    }

    @Test
    void shouldReturnEmptyWhenMessageIdIsMissing() {
        final MailtrapClient mailtrapClient = mock(MailtrapClient.class);
        final SendResponse response = new SendResponse();
        response.setSuccess(true);
        response.setMessageIds(List.of());
        when(mailtrapClient.send(any(MailtrapMail.class))).thenReturn(response);

        final EmailClient client = new MailtrapEmailClient(mailtrapClient);
        final EmailMessage email = new EmailMessage(
            EmailMessageType.CUSTOMER_PASSWORD_RESET,
            new Email("jack@kartoush.com"),
            new Email("no-reply@notify.kartoush.com"),
            "Kartoush",
            "Reset your Kartoush password",
            "Reset it here",
            "https://kartoush.dev/reset-password?token=abc",
            "<p><a href=\"https://kartoush.dev/reset-password?token=abc\">Reset</a></p>"
        );

        assertThat(client.send(email)).isEmpty();
    }

    @Test
    void shouldWrapSdkFailuresInEmailDeliveryException() {
        final MailtrapClient mailtrapClient = mock(MailtrapClient.class);
        when(mailtrapClient.send(any(MailtrapMail.class))).thenThrow(new IllegalStateException("mailtrap boom"));

        final EmailClient client = new MailtrapEmailClient(mailtrapClient);
        final EmailMessage email = new EmailMessage(
            EmailMessageType.CUSTOMER_PASSWORD_RESET,
            new Email("jack@kartoush.com"),
            new Email("no-reply@notify.kartoush.com"),
            "Kartoush",
            "Reset your Kartoush password",
            "Reset it here",
            "https://kartoush.dev/reset-password?token=abc",
            "<p><a href=\"https://kartoush.dev/reset-password?token=abc\">Reset</a></p>"
        );

        assertThatThrownBy(() -> client.send(email))
            .isInstanceOfSatisfying(EmailDeliveryException.class, exception -> {
                assertThat(exception.provider()).isEqualTo("mailtrap");
                assertThat(exception).hasMessage("Mailtrap email delivery failed");
                assertThat(exception).hasCauseInstanceOf(IllegalStateException.class);
            });
    }
}
