package com.kartoush.auth.email;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DefaultBrevoHttpClient implements BrevoHttpClient {

    private final HttpClient httpClient;

    public DefaultBrevoHttpClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public HttpResponse<String> send(final HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (final IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new EmailDeliveryException("Brevo email delivery failed", exception);
        }
    }
}
