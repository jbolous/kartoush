package com.kartoush.notification;

import com.kartoush.notification.email.EmailMessage;
import com.kartoush.jobs.monitoring.BackgroundJobRecoveryMonitor;
import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jobrunr.storage.StorageProvider;
import com.kartoush.notification.email.EmailMessageType;
import com.kartoush.notification.email.client.EmailClient;
import com.kartoush.notification.email.delivery.EmailDeliveryException;
import com.kartoush.notification.email.delivery.EmailDeliveryRecoveryService;
import com.kartoush.notification.email.delivery.EmailDeliveryOutcomeException;
import com.kartoush.notification.email.delivery.EmailDeliveryReconciliationService;
import com.kartoush.notification.email.delivery.TrackedEmailDeliveryService;
import com.kartoush.notification.persistence.repository.EmailDeliveryRepository;
import com.kartoush.platform.types.Email;
import com.kartoush.testsupport.PostgresSpringIntegrationTest;
import com.kartoush.testsupport.SpringIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

@SpringIntegrationTest
class EmailDeliveryTrackingIntegrationTest extends PostgresSpringIntegrationTest {
    @Autowired private EmailDeliveryRepository repository;
    @Autowired private PlatformTransactionManager manager;
    @Autowired private EmailDeliveryReconciliationService reconciliation;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EmailDeliveryRecoveryService recovery;
    @Autowired private BackgroundJobOutboxRepository outbox;
    @Autowired private StorageProvider storage;

    @Test
    void shouldKeepAcceptanceWhenCallingTransactionRollsBack() {
        final var client = mock(EmailClient.class);
        when(client.send(any())).thenReturn(Optional.of("provider-receipt"));
        final var email = email();
        final var service = service(client, Clock.systemUTC());
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            service.send(email);
            status.setRollbackOnly();
        });
        service.send(email);
        verify(client, times(1)).send(email);
        assertThat(status(email)).isEqualTo("ACCEPTED");
        assertThat(jdbc.queryForObject("select provider_message_id from email_delivery where id = ?",
            String.class, id(email))).isEqualTo("provider-receipt");
        assertThat(attempts(email)).isEqualTo(1);
    }

    @Test
    void shouldHoldAmbiguousFailureUntilNonAcceptanceIsConfirmed() {
        final var client = mock(EmailClient.class);
        when(client.send(any())).thenThrow(new EmailDeliveryException("mailtrap", "response lost"))
            .thenReturn(Optional.of("receipt-after-confirmed-non-acceptance"));
        final var email = email();
        final var service = service(client, Clock.systemUTC());
        assertThatThrownBy(() -> service.send(email)).isInstanceOf(EmailDeliveryException.class);
        assertThat(status(email)).isEqualTo("UNKNOWN");
        assertThatThrownBy(() -> service.send(email)).hasMessageContaining("UNKNOWN");
        verify(client, times(1)).send(any());
        reconciliation.reconcile(id(email), false, null, "support-case-confirmed-not-accepted");
        service.send(email);
        assertThat(status(email)).isEqualTo("ACCEPTED");
        assertThat(attempts(email)).isEqualTo(2);
    }

    @Test
    void shouldSkipResendWhenReconciliationConfirmsAcceptance() {
        final var client = mock(EmailClient.class);
        when(client.send(any())).thenThrow(new EmailDeliveryException("mailtrap", "response lost"));
        final var email = email();
        final var service = service(client, Clock.systemUTC());
        assertThatThrownBy(() -> service.send(email)).isInstanceOf(EmailDeliveryException.class);
        reconciliation.reconcile(id(email), true, "recovered-receipt", "provider-support-case");
        service.send(email);
        verify(client, times(1)).send(any());
        assertThat(status(email)).isEqualTo("ACCEPTED");
    }

    @Test
    void shouldHoldCrashedAttemptAfterItBecomesStale() {
        final var email = email();
        final var now = Instant.now();
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            repository.insertIfAbsent(id(email), "mailtrap", now);
            final var delivery = repository.lockById(id(email)).orElseThrow();
            delivery.startAttempt(now);
            repository.recordAttempt(id(email), 1, now);
        });
        final var client = mock(EmailClient.class);
        final var service = service(client, Clock.fixed(now.plusSeconds(301), ZoneOffset.UTC));
        assertThatThrownBy(() -> service.send(email)).hasMessageContaining("UNKNOWN");
        assertThat(status(email)).isEqualTo("UNKNOWN");
        verifyNoInteractions(client);
    }

    @Test
    void shouldPreventConcurrentSendWhileProviderCallIsInFlight() throws Exception {
        final var client = mock(EmailClient.class);
        final var entered = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        when(client.send(any())).thenAnswer(invocation -> {
            entered.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test timed out");
            }
            return Optional.of("receipt");
        });
        final var email = email();
        final var service = service(client, Clock.systemUTC());
        final var first = CompletableFuture.runAsync(() -> service.send(email));
        try {
            assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> service.send(email)).hasMessageContaining("SENDING");
            assertThatThrownBy(() -> reconciliation.reconcile(id(email), false, null, "premature"))
                .isInstanceOf(IllegalStateException.class);
        } finally {
            release.countDown();
        }
        first.get(10, TimeUnit.SECONDS);
        verify(client, times(1)).send(any());
    }

    @Test
    void shouldRetryOnlyDefinitiveRetryableRejection() {
        final var client = mock(EmailClient.class);
        when(client.send(any())).thenThrow(new EmailDeliveryOutcomeException("mailtrap", "not accepted", true))
            .thenReturn(Optional.empty());
        final var email = email();
        final var service = service(client, Clock.systemUTC());
        assertThatThrownBy(() -> service.send(email)).isInstanceOf(EmailDeliveryOutcomeException.class);
        assertThat(status(email)).isEqualTo("RETRYABLE");
        service.send(email);
        assertThat(status(email)).isEqualTo("ACCEPTED");
        assertThat(attempts(email)).isEqualTo(2);
    }

    @Test
    void shouldHoldDefinitivePermanentRejection() {
        final var client = mock(EmailClient.class);
        when(client.send(any())).thenThrow(new EmailDeliveryOutcomeException("mailtrap", "rejected", false));
        final var email = email();
        final var service = service(client, Clock.systemUTC());
        assertThatThrownBy(() -> service.send(email)).isInstanceOf(EmailDeliveryOutcomeException.class);
        assertThatThrownBy(() -> service.send(email)).hasMessageContaining("FAILED");
        verify(client, times(1)).send(any());
    }

    @Test
    void shouldHoldAttemptWhenReceiptCannotBeCommittedAfterProviderAcceptance() {
        final var email = email();
        final var client = mock(EmailClient.class);
        when(client.send(any())).thenReturn(Optional.of("accepted-but-receipt-write-failed"));
        final var failingRepository = mock(EmailDeliveryRepository.class, delegatesTo(repository));
        doThrow(new IllegalStateException("database unavailable"))
            .when(failingRepository).recordOutcome(eq(id(email)), eq(1), eq("ACCEPTED"), any(), any());
        final var service = new TrackedEmailDeliveryService("mailtrap", client, failingRepository,
            manager, Clock.systemUTC());

        assertThatThrownBy(() -> service.send(email)).hasMessageContaining("database unavailable");
        assertThat(status(email)).isEqualTo("SENDING");
        assertThatThrownBy(() -> service.send(email)).hasMessageContaining("SENDING");
        final var later = service(client, Clock.fixed(Instant.now().plusSeconds(301), ZoneOffset.UTC));
        assertThatThrownBy(() -> later.send(email)).hasMessageContaining("UNKNOWN");
        verify(client, times(1)).send(any());
    }

    @Test
    void shouldReuseStableIdentityWhenLegacyCallerHasNoKey() {
        final var keyed = email();
        final var email = new EmailMessage(keyed.type(), keyed.recipient(), keyed.senderAddress(),
            keyed.senderName(), keyed.subject(), keyed.textBody(), keyed.actionUrl(), keyed.htmlBody());
        final var client = mock(EmailClient.class);
        when(client.send(any())).thenReturn(Optional.of("legacy-receipt"));
        final var service = service(client, Clock.systemUTC());
        service.send(email);
        service.send(email);
        verify(client, times(1)).send(any());
    }

    @Test
    void shouldMarkOnlyStaleAttemptsUnknownWhenRecoveryRunsWithoutJobRetry() {
        final var stale = email();
        final var fresh = email();
        final var old = Instant.now().minusSeconds(301);
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            for (final var email : java.util.List.of(stale, fresh)) {
                final var startedAt = email == stale ? old : Instant.now();
                repository.insertIfAbsent(id(email), "mailtrap", startedAt);
                repository.lockById(id(email)).orElseThrow().startAttempt(startedAt);
                repository.recordAttempt(id(email), 1, startedAt);
            }
        });

        final var metrics = new SimpleMeterRegistry();
        new BackgroundJobRecoveryMonitor(recovery, outbox, storage, Clock.systemUTC(), metrics).check();
        assertThat(metrics.get("jobs.recovery.monitor.failures").counter().count()).isZero();
        assertThat(metrics.get("jobs.recovery.unresolved").tag("kind", "email-unknown").gauge().value())
            .isGreaterThanOrEqualTo(1);
        assertThat(status(stale)).isEqualTo("UNKNOWN");
        assertThat(status(fresh)).isEqualTo("SENDING");
        assertThat(jdbc.queryForObject("select status from email_delivery_attempt where delivery_id = ?",
            String.class, id(stale))).isEqualTo("UNKNOWN");
        final var client = mock(EmailClient.class);
        assertThatThrownBy(() -> service(client, Clock.systemUTC()).send(stale)).hasMessageContaining("UNKNOWN");
        verifyNoInteractions(client);
    }

    private TrackedEmailDeliveryService service(final EmailClient client, final Clock clock) {
        return new TrackedEmailDeliveryService("mailtrap", client, repository, manager, clock);
    }

    private EmailMessage email() {
        return new EmailMessage(EmailMessageType.CUSTOMER_ACTIVATION, new Email("recipient@example.com"),
            new Email("sender@example.com"), "Kartoush", "Activate", "Activate your account",
            "https://example.com/activate").withIdempotencyKey(UUID.randomUUID().toString());
    }

    private UUID id(final EmailMessage email) { return UUID.fromString(email.idempotencyKey()); }

    private String status(final EmailMessage email) {
        return jdbc.queryForObject("select status from email_delivery where id = ?", String.class, id(email));
    }

    private int attempts(final EmailMessage email) {
        return jdbc.queryForObject("select count(*) from email_delivery_attempt where delivery_id = ?", Integer.class, id(email));
    }
}
