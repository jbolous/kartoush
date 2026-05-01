package com.kartoush.auth.email;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultMailtrapEmailApiClient implements MailtrapEmailApiClient {

    private static final Pattern MESSAGE_IDS_PATTERN = Pattern.compile("\"message_ids\"\\s*:\\s*\\[\\s*\"([^\"]+)\"");

    private final MailtrapHttpClient mailtrapHttpClient;
    private final EmailDeliveryProperties.Mailtrap properties;

    public DefaultMailtrapEmailApiClient(
        final MailtrapHttpClient mailtrapHttpClient,
        final EmailDeliveryProperties.Mailtrap properties
    ) {
        this.mailtrapHttpClient = mailtrapHttpClient;
        this.properties = properties;
    }

    @Override
    public Optional<String> send(final EmailMessage email) {
        final String endpoint = properties.getApiBaseUrl().replaceAll("/+$", "") + "/" + properties.getInboxId();
        final HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(10))
            .header("accept", "application/json")
            .header("content-type", "application/json")
            .header("Api-Token", properties.getApiToken())
            .POST(HttpRequest.BodyPublishers.ofString(toJson(email)))
            .build();

        final HttpResponse<String> response = mailtrapHttpClient.send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new EmailDeliveryException("Mailtrap email delivery failed with status " + response.statusCode());
        }

        return extractMessageId(response.body());
    }

    private String toJson(final EmailMessage email) {
        return """
            {
              "from": {
                "email": "%s",
                "name": "%s"
              },
              "to": [
                {
                  "email": "%s"
                }
              ],
              "subject": "%s",
              "text": "%s"
            }
            """.formatted(
            escape(email.senderAddress().value()),
            escape(email.senderName()),
            escape(email.recipient().value()),
            escape(email.subject()),
            escape(email.textBody())
        ).trim();
    }

    private Optional<String> extractMessageId(final String body) {
        final Matcher matcher = MESSAGE_IDS_PATTERN.matcher(body);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
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
