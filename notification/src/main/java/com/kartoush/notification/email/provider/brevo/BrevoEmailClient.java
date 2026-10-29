package com.kartoush.notification.email.provider.brevo;

import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.client.EmailClient;
import com.kartoush.notification.email.config.EmailDeliveryProperties;
import com.kartoush.notification.email.delivery.EmailDeliveryException;
import com.kartoush.notification.email.http.NotificationHttpClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Map;

public class BrevoEmailClient implements EmailClient {

    private static final String PROVIDER = "brevo";

    private static final Logger LOG = LoggerFactory.getLogger(BrevoEmailClient.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final NotificationHttpClient notificationHttpClient;

    private final EmailDeliveryProperties.Brevo properties;

    public BrevoEmailClient(
        final NotificationHttpClient notificationHttpClient,
        final EmailDeliveryProperties.Brevo properties
    ) {
        this.notificationHttpClient = notificationHttpClient;
        this.properties = properties;
    }

    @Override
    public Optional<String> send(final EmailMessage email) {
        final String endpoint = properties.getApiBaseUrl().replaceAll("/+$", "") + "/smtp/email";
        final HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(10))
            .header("accept", "application/json")
            .header("content-type", "application/json")
            .header("api-key", properties.getApiKey())
            .POST(HttpRequest.BodyPublishers.ofString(toJsonBody(email)))
            .build();

        final HttpResponse<String> response = notificationHttpClient.send(request, PROVIDER);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            if (email.idempotencyKey() != null && isIdempotencyDuplicate(response)) {
                return Optional.empty();
            }
            LOG.error(
                "Brevo email delivery failed with status={}",
                response.statusCode()
            );
            throw new EmailDeliveryException(PROVIDER, "Brevo email delivery failed with status " + response.statusCode());
        }

        return extractMessageId(response.body());
    }

    private String toJsonBody(final EmailMessage email) {
        try {
            return OBJECT_MAPPER.writeValueAsString(
                new BrevoSendEmailRequest(
                    new Sender(email.senderName(), email.senderAddress().value()),
                    List.of(new Recipient(email.recipient().value())),
                    email.subject(),
                    email.textBody(),
                    email.htmlBody(),
                    email.idempotencyKey() == null ? null : Map.of("idempotencyKey", email.idempotencyKey())
                )
            );
        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new EmailDeliveryException(PROVIDER, "Brevo email delivery failed", exception);
        }
    }

    private Optional<String> extractMessageId(final String body) {
        try {
            final BrevoSendEmailResponse response = OBJECT_MAPPER.readValue(body, BrevoSendEmailResponse.class);
            if (response.messageId() != null && !response.messageId().isBlank()) {
                return Optional.of(response.messageId());
            }

            if (response.messageIds() != null && !response.messageIds().isEmpty()) {
                return Optional.ofNullable(response.messageIds().getFirst());
            }

            return Optional.empty();
        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new EmailDeliveryException(PROVIDER, "Brevo email delivery failed", exception);
        }
    }

    private boolean isIdempotencyDuplicate(final HttpResponse<String> response) {
        if (response.statusCode() != 400) {
            return false;
        }
        try {
            final var error = OBJECT_MAPPER.readTree(response.body());
            return "duplicate_parameter".equals(error.path("code").asText())
                && error.path("message").asText().toLowerCase(java.util.Locale.ROOT).contains("idempotency");
        } catch (final Exception ex) {
            return false;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record BrevoSendEmailRequest(
        Sender sender,
        List<Recipient> to,
        String subject,
        String textContent,
        String htmlContent,
        Map<String, String> headers
    ) {
    }

    private record Sender(String name, String email) {
    }

    private record Recipient(String email) {
    }

    private record BrevoSendEmailResponse(
        String messageId,
        List<String> messageIds
    ) {
    }
}
