package com.kartoush.notification.sms.provider.twilio;

import com.kartoush.notification.sms.SmsMessage;
import com.kartoush.notification.sms.config.SmsDeliveryProperties;
import com.kartoush.notification.sms.delivery.SmsDeliveryException;
import com.kartoush.notification.sms.delivery.SmsDeliveryService;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class TwilioSmsDeliveryService implements SmsDeliveryService {
    private final HttpClient httpClient;
    private final SmsDeliveryProperties properties;

    public TwilioSmsDeliveryService(final HttpClient httpClient, final SmsDeliveryProperties properties) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient is required");
        this.properties = Objects.requireNonNull(properties, "properties are required");
        if (!properties.enabled()) {
            throw new IllegalArgumentException("Twilio SMS delivery must be enabled");
        }
    }

    @Override
    public void send(final SmsMessage message) {
        Objects.requireNonNull(message, "message is required");
        final String credentials = Base64.getEncoder().encodeToString(
            (properties.accountSid() + ":" + properties.authToken()).getBytes(StandardCharsets.UTF_8));
        final String body = "To=" + encode(message.recipient().value())
            + "&From=" + encode(properties.fromNumber()) + "&Body=" + encode(message.body());
        final HttpRequest request = HttpRequest.newBuilder(URI.create(
                "https://api.twilio.com/2010-04-01/Accounts/" + properties.accountSid() + "/Messages.json"))
            .timeout(properties.timeout())
            .header("Authorization", "Basic " + credentials)
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        try {
            final HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 201) {
                throw new SmsDeliveryException("Twilio SMS request failed with HTTP status " + response.statusCode());
            }
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SmsDeliveryException("Twilio SMS request interrupted", ex);
        } catch (final IOException ex) {
            throw new SmsDeliveryException("Twilio SMS request failed", ex);
        }
    }

    private static String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
