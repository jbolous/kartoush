package com.kartoush.notification.email.provider.brevo;

import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.EmailMessageType;
import com.kartoush.notification.email.client.EmailClient;
import com.kartoush.notification.email.config.EmailDeliveryProperties;
import com.kartoush.notification.email.delivery.EmailDeliveryException;
import com.kartoush.notification.email.http.NotificationHttpClient;
import com.kartoush.platform.types.Email;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLSession;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrevoEmailClientTest {

    @Test
    void shouldBuildBrevoRequestAndExtractMessageId() {
        final NotificationHttpClient notificationHttpClient = mock(NotificationHttpClient.class);
        final EmailDeliveryProperties.Brevo properties = new EmailDeliveryProperties.Brevo();
        properties.setApiBaseUrl("https://api.brevo.com/v3");
        properties.setApiKey("brevo-api-key");

        final HttpRequestCapturingResponse response = new HttpRequestCapturingResponse(
            201,
            "{\"messageId\":\"brevo-message-id\"}"
        );
        when(notificationHttpClient.send(any(HttpRequest.class), any(String.class))).thenAnswer(invocation -> {
            response.setRequest(invocation.getArgument(0));
            return response;
        });

        final EmailClient client = new BrevoEmailClient(notificationHttpClient, properties);
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

        assertThat(messageId).contains("brevo-message-id");
        assertThat(response.request.uri()).isEqualTo(URI.create("https://api.brevo.com/v3/smtp/email"));
        assertThat(response.request.headers().firstValue("api-key")).contains("brevo-api-key");
        assertThat(response.request.headers().firstValue("content-type")).contains("application/json");
        assertThat(response.request.method()).isEqualTo("POST");
        assertThat(response.request.bodyPublisher()).isPresent();
        assertThat(bodyToString(response.request.bodyPublisher().orElseThrow()))
            .contains("\"subject\":\"Activate your Kartoush account\"")
            .contains("\"textContent\":\"Click here\"")
            .contains("\"htmlContent\":\"<p><a href=\\\"https://kartoush.dev/activate?token=abc\\\">Activate</a></p>\"");
    }

    @Test
    void shouldExtractFirstMessageIdFromMessageIdsArray() {
        final NotificationHttpClient notificationHttpClient = mock(NotificationHttpClient.class);
        final EmailDeliveryProperties.Brevo properties = new EmailDeliveryProperties.Brevo();
        properties.setApiBaseUrl("https://api.brevo.com/v3");
        properties.setApiKey("brevo-api-key");

        when(notificationHttpClient.send(any(HttpRequest.class), any(String.class)))
            .thenReturn(new HttpRequestCapturingResponse(201, "{\"messageIds\":[\"brevo-message-id\",\"other-id\"]}"));

        final EmailClient client = new BrevoEmailClient(notificationHttpClient, properties);
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

        final Optional<String> messageId = client.send(email);

        assertThat(messageId).contains("brevo-message-id");
    }

    @Test
    void shouldReturnEmptyWhenBrevoResponseHasNoMessageId() {
        final NotificationHttpClient notificationHttpClient = mock(NotificationHttpClient.class);
        final EmailDeliveryProperties.Brevo properties = new EmailDeliveryProperties.Brevo();
        properties.setApiBaseUrl("https://api.brevo.com/v3");
        properties.setApiKey("brevo-api-key");

        when(notificationHttpClient.send(any(HttpRequest.class), any(String.class)))
            .thenReturn(new HttpRequestCapturingResponse(201, "{}"));

        final EmailClient client = new BrevoEmailClient(notificationHttpClient, properties);
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

        final Optional<String> messageId = client.send(email);

        assertThat(messageId).isEmpty();
    }

    @Test
    void shouldThrowWhenBrevoReturnsNonSuccessStatus() {
        final NotificationHttpClient notificationHttpClient = mock(NotificationHttpClient.class);
        final EmailDeliveryProperties.Brevo properties = new EmailDeliveryProperties.Brevo();
        properties.setApiBaseUrl("https://api.brevo.com/v3");
        properties.setApiKey("brevo-api-key");

        when(notificationHttpClient.send(any(HttpRequest.class), any(String.class)))
            .thenReturn(new HttpRequestCapturingResponse(500, "{\"message\":\"provider failure\"}"));

        final EmailClient client = new BrevoEmailClient(notificationHttpClient, properties);
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
                assertThat(exception.provider()).isEqualTo("brevo");
                assertThat(exception).hasMessage("Brevo email delivery failed with status 500");
            });
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
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

    }

    private String bodyToString(final BodyPublisher bodyPublisher) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final CountDownLatch latch = new CountDownLatch(1);
        final CompletableFuture<Throwable> error = new CompletableFuture<>();

        bodyPublisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(final Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final ByteBuffer item) {
                final byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                output.writeBytes(bytes);
            }

            @Override
            public void onError(final Throwable throwable) {
                error.complete(throwable);
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(error).isNotCompletedExceptionally();
            assertThat(error).isNotDone();
            return output.toString(StandardCharsets.UTF_8);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while reading request body", exception);
        }
    }
}
