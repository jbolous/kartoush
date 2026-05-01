package com.kartoush.auth.email;

import com.kartoush.platform.types.Email;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultMailtrapEmailApiClientTest {

    @Test
    void shouldBuildMailtrapRequestAndExtractMessageId() {
        final MailtrapHttpClient mailtrapHttpClient = mock(MailtrapHttpClient.class);
        final EmailDeliveryProperties.Mailtrap properties = new EmailDeliveryProperties.Mailtrap();
        properties.setApiBaseUrl("https://sandbox.api.mailtrap.io/api/send");
        properties.setApiToken("mailtrap-api-token");
        properties.setInboxId(12345);

        final HttpRequestCapturingResponse response = new HttpRequestCapturingResponse(
            200,
            "{\"message_ids\":[\"mailtrap-message-id\"]}"
        );
        when(mailtrapHttpClient.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            response.setRequest(invocation.getArgument(0));
            return response;
        });

        final DefaultMailtrapEmailApiClient client = new DefaultMailtrapEmailApiClient(mailtrapHttpClient, properties);
        final EmailMessage email = new EmailMessage(
            EmailMessageType.CUSTOMER_ACTIVATION,
            new Email("jack@kartoush.com"),
            new Email("no-reply@notify.kartoush.com"),
            "Kartoush",
            "Activate your Kartoush account",
            "Click here",
            "https://kartoush.dev/activate?token=abc"
        );

        final Optional<String> messageId = client.send(email);

        assertThat(messageId).contains("mailtrap-message-id");
        assertThat(response.request.uri()).isEqualTo(URI.create("https://sandbox.api.mailtrap.io/api/send/12345"));
        assertThat(response.request.headers().firstValue("Api-Token")).contains("mailtrap-api-token");
        assertThat(response.request.headers().firstValue("content-type")).contains("application/json");
        assertThat(response.request.method()).isEqualTo("POST");
    }

    @Test
    void shouldThrowWhenMailtrapReturnsNonSuccessStatus() {
        final MailtrapHttpClient mailtrapHttpClient = mock(MailtrapHttpClient.class);
        final EmailDeliveryProperties.Mailtrap properties = new EmailDeliveryProperties.Mailtrap();
        properties.setApiBaseUrl("https://sandbox.api.mailtrap.io/api/send");
        properties.setApiToken("mailtrap-api-token");
        properties.setInboxId(12345);

        when(mailtrapHttpClient.send(any(HttpRequest.class)))
            .thenReturn(new HttpRequestCapturingResponse(401, "{\"message\":\"unauthorized\"}"));

        final DefaultMailtrapEmailApiClient client = new DefaultMailtrapEmailApiClient(mailtrapHttpClient, properties);
        final EmailMessage email = new EmailMessage(
            EmailMessageType.CUSTOMER_PASSWORD_RESET,
            new Email("jack@kartoush.com"),
            new Email("no-reply@notify.kartoush.com"),
            "Kartoush",
            "Reset your Kartoush password",
            "Reset it here",
            "https://kartoush.dev/reset-password?token=abc"
        );

        assertThatThrownBy(() -> client.send(email))
            .isInstanceOf(EmailDeliveryException.class)
            .hasMessage("Mailtrap email delivery failed with status 401");
    }

    static class HttpRequestCapturingResponse implements HttpResponse<String> {

        private final int statusCode;
        private final String body;
        private HttpRequest request;

        HttpRequestCapturingResponse(final int statusCode, final String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        void setRequest(final HttpRequest request) {
            this.request = request;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public Optional<javax.net.ssl.SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public java.net.http.HttpClient.Version version() {
            return java.net.http.HttpClient.Version.HTTP_1_1;
        }
    }
}
