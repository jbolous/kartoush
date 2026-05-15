package com.kartoush.notification.email.provider.mailtrap;

import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.client.EmailClient;
import com.kartoush.notification.email.delivery.EmailDeliveryException;
import io.mailtrap.client.MailtrapClient;
import io.mailtrap.model.request.emails.Address;
import io.mailtrap.model.request.emails.MailtrapMail;
import io.mailtrap.model.response.emails.SendResponse;

import java.util.List;
import java.util.Optional;

public class MailtrapEmailClient implements EmailClient {

    private static final String PROVIDER = "mailtrap";

    private final MailtrapClient mailtrapClient;

    public MailtrapEmailClient(final MailtrapClient mailtrapClient) {
        this.mailtrapClient = mailtrapClient;
    }

    @Override
    public Optional<String> send(final EmailMessage email) {
        final MailtrapMail.MailtrapMailBuilder mailBuilder = MailtrapMail.builder()
            .from(new Address(email.senderAddress().value(), email.senderName()))
            .to(List.of(new Address(email.recipient().value())))
            .subject(email.subject())
            .text(email.textBody());

        if (email.htmlBody() != null) {
            mailBuilder.html(email.htmlBody());
        }

        final MailtrapMail mailtrapMail = mailBuilder.build();

        try {
            final SendResponse response = mailtrapClient.send(mailtrapMail);
            if (!response.isSuccess()) {
                throw new EmailDeliveryException(PROVIDER, "Mailtrap email delivery failed");
            }

            return Optional.ofNullable(response.getMessageIds())
                .stream()
                .flatMap(List::stream)
                .findFirst();
        }
        catch (final RuntimeException exception) {
            if (exception instanceof EmailDeliveryException deliveryException) {
                throw deliveryException;
            }

            throw new EmailDeliveryException(PROVIDER, "Mailtrap email delivery failed", exception);
        }
    }
}
