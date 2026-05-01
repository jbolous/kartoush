package com.kartoush.auth.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultBrevoEmailApiClient implements BrevoEmailApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultBrevoEmailApiClient.class);
    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("\"messageId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MESSAGE_IDS_PATTERN = Pattern.compile("\"messageIds\"\\s*:\\s*\\[\\s*\"([^\"]+)\"");

    private final BrevoHttpClient brevoHttpClient;
    private final EmailDeliveryProperties.Brevo properties;

    public DefaultBrevoEmailApiClient(
        final BrevoHttpClient brevoHttpClient,
        final EmailDeliveryProperties.Brevo properties
    ) {
        this.brevoHttpClient = brevoHttpClient;
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
            .POST(HttpRequest.BodyPublishers.ofString(toJson(email)))
            .build();

        final HttpResponse<String> response = brevoHttpClient.send(request);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOG.error(
                "Brevo email delivery failed with status={} body={}",
                response.statusCode(),
                response.body()
            );
            throw new EmailDeliveryException("Brevo email delivery failed with status " + response.statusCode());
        }

        return extractMessageId(response.body());
    }

    private String toJson(final EmailMessage email) {
        return """
            {
              "sender": {
                "name": "%s",
                "email": "%s"
              },
              "to": [
                {
                  "email": "%s"
                }
              ],
              "subject": "%s",
              "textContent": "%s"
            }
            """.formatted(
            escape(email.senderName()),
            escape(email.senderAddress().value()),
            escape(email.recipient().value()),
            escape(email.subject()),
            escape(email.textBody())
        ).trim();
    }

    private Optional<String> extractMessageId(final String body) {
        final Matcher messageIdMatcher = MESSAGE_ID_PATTERN.matcher(body);
        if (messageIdMatcher.find()) {
            return Optional.of(messageIdMatcher.group(1));
        }

        final Matcher messageIdsMatcher = MESSAGE_IDS_PATTERN.matcher(body);
        if (messageIdsMatcher.find()) {
            return Optional.of(messageIdsMatcher.group(1));
        }

        return Optional.empty();
    }

    private String escape(final String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }
}
