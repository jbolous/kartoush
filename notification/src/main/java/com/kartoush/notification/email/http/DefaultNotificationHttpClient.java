package com.kartoush.notification.email.http;

import com.kartoush.notification.email.delivery.EmailDeliveryException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DefaultNotificationHttpClient implements NotificationHttpClient {

    private final HttpClient httpClient;

    public DefaultNotificationHttpClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public HttpResponse<String> send(final HttpRequest request, final String provider) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (final IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new EmailDeliveryException(provider, "Notification email delivery failed", exception);
        }
    }
}
