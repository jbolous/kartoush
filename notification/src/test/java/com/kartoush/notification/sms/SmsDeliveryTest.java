package com.kartoush.notification.sms;

import com.kartoush.notification.sms.config.SmsDeliveryConfiguration;
import com.kartoush.notification.sms.config.SmsDeliveryProperties;
import com.kartoush.notification.sms.delivery.NoOpSmsDeliveryService;
import com.kartoush.notification.sms.delivery.SmsDeliveryException;
import com.kartoush.notification.sms.provider.twilio.TwilioSmsDeliveryService;
import com.kartoush.platform.types.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsDeliveryTest {
    private static final String ACCOUNT_SID = "AC" + "a".repeat(32);
    private final HttpClient httpClient = mock(HttpClient.class);
    private final SmsDeliveryProperties properties = new SmsDeliveryProperties(
        true, ACCOUNT_SID, "secret-token", "+13125550001", Duration.ofSeconds(10));
    private final SmsMessage message = new SmsMessage(new PhoneNumber("+13125550002"), "Code: 123 & ready + go");
    private TwilioSmsDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new TwilioSmsDeliveryService(httpClient, properties);
    }

    @Test
    void shouldBindDisabledDefaultsWithoutCredentials() {
        new ApplicationContextRunner().withUserConfiguration(SmsDeliveryConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(NoOpSmsDeliveryService.class);
            assertThat(context.getBean(SmsDeliveryProperties.class).enabled()).isFalse();
        });
    }

    @Test
    void shouldFailStartupWhenEnabledWithoutCredentials() {
        new ApplicationContextRunner().withUserConfiguration(SmsDeliveryConfiguration.class)
            .withPropertyValues("kartoush.sms.delivery.enabled=true")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldEncodeAndSubmitSmsToTwilio() throws Exception {
        final HttpResponse<Void> response = response(201);
        when(httpClient.<Void>send(any(), any())).thenReturn(response);

        service.send(message);

        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        final HttpRequest request = captor.getValue();
        assertThat(request.uri().toString()).isEqualTo(
            "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json");
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.timeout()).contains(Duration.ofSeconds(10));
        assertThat(request.headers().firstValue("Authorization")).hasValueSatisfying(value ->
            assertThat(value).startsWith("Basic ").doesNotContain("secret-token"));
        assertThat(readBody(request)).isEqualTo(
            "To=%2B13125550002&From=%2B13125550001&Body=Code%3A+123+%26+ready+%2B+go");
    }

    @Test
    void shouldReportProviderFailureWithoutExposingMessageOrCredentials() throws Exception {
        final HttpResponse<Void> response = response(429);
        when(httpClient.<Void>send(any(), any())).thenReturn(response);

        assertThatThrownBy(() -> service.send(message)).isInstanceOf(SmsDeliveryException.class)
            .hasMessage("Twilio SMS request failed with HTTP status 429");
    }

    @Test
    void shouldWrapNetworkFailures() throws Exception {
        when(httpClient.<Void>send(any(), any())).thenThrow(new IOException("network unavailable"));

        assertThatThrownBy(() -> service.send(message)).isInstanceOf(SmsDeliveryException.class)
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void shouldRestoreInterruptFlagWhenInterrupted() throws Exception {
        when(httpClient.<Void>send(any(), any())).thenThrow(new InterruptedException());
        try {
            assertThatThrownBy(() -> service.send(message)).isInstanceOf(SmsDeliveryException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void shouldRejectInvalidSmsDataAndRedactSensitiveRepresentations() {
        assertThatThrownBy(() -> new SmsMessage(new PhoneNumber("13125550002"), "Hello"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SmsMessage(message.recipient(), " "))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SmsMessage(message.recipient(), "x".repeat(1601)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(message.toString()).doesNotContain("13125550002", "123");
        assertThat(properties.toString()).doesNotContain("secret-token", ACCOUNT_SID, "13125550001");
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<Void> response(final int status) {
        final HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        return response;
    }

    private String readBody(final HttpRequest request) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final CompletableFuture<String> result = new CompletableFuture<>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            public void onSubscribe(final Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(final ByteBuffer buffer) {
                final byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            public void onError(final Throwable error) { result.completeExceptionally(error); }
            public void onComplete() { result.complete(bytes.toString(StandardCharsets.UTF_8)); }
        });
        return result.join();
    }
}
