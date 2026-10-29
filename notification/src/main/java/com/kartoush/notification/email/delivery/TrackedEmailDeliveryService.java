package com.kartoush.notification.email.delivery;

import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.client.EmailClient;
import com.kartoush.notification.persistence.entity.EmailDeliveryEntity;
import com.kartoush.notification.persistence.repository.EmailDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Keeps provider acceptance durable independently of the calling business/job transaction. */
public class TrackedEmailDeliveryService implements EmailDeliveryService {
    private static final Logger LOG = LoggerFactory.getLogger(TrackedEmailDeliveryService.class);
    private static final Duration STALE_AFTER = Duration.ofMinutes(5);
    private final String provider;
    private final EmailClient client;
    private final EmailDeliveryRepository repository;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public TrackedEmailDeliveryService(final String provider, final EmailClient client,
        final EmailDeliveryRepository repository, final PlatformTransactionManager manager, final Clock clock) {
        this.provider = provider;
        this.client = client;
        this.repository = repository;
        this.clock = clock;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void send(final EmailMessage email) {
        // Existing callers without an event key use a stable content identity; no email content is persisted here.
        final UUID id = email.idempotencyKey() == null
            ? UUID.nameUUIDFromBytes(email.toString().getBytes(StandardCharsets.UTF_8))
            : UUID.fromString(email.idempotencyKey());
        final Claim claim = transaction.execute(status -> {
            final Instant now = clock.instant();
            repository.insertIfAbsent(id, provider, now);
            final EmailDeliveryEntity delivery = repository.lockById(id).orElseThrow();
            if (!provider.equals(delivery.getProvider())) {
                throw new IllegalStateException("Email delivery provider changed for delivery " + id);
            }
            if (delivery.getStatus() == EmailDeliveryStatus.SENDING
                && !delivery.getAttemptStartedAt().plus(STALE_AFTER).isAfter(now)) {
                delivery.finish(EmailDeliveryStatus.UNKNOWN, null, now);
                repository.recordOutcome(id, delivery.getAttemptCount(), "UNKNOWN", null, now);
            }
            if (delivery.getStatus() != EmailDeliveryStatus.RETRYABLE) {
                return new Claim(delivery.getStatus(), delivery.getAttemptCount());
            }
            delivery.startAttempt(now);
            repository.recordAttempt(id, delivery.getAttemptCount(), now);
            return new Claim(EmailDeliveryStatus.RETRYABLE, delivery.getAttemptCount());
        });
        if (claim.status() == EmailDeliveryStatus.ACCEPTED) {
            return;
        }
        if (claim.status() != EmailDeliveryStatus.RETRYABLE) {
            LOG.warn("Email delivery held: deliveryId={} provider={} status={}", id, provider, claim.status());
            throw new EmailDeliveryException(provider, "Email delivery " + id + " is held in status " + claim.status());
        }

        final String receipt;
        try {
            receipt = client.send(email.withIdempotencyKey(id.toString())).orElse(null);
        } catch (final RuntimeException exception) {
            final EmailDeliveryStatus outcome = exception instanceof EmailDeliveryOutcomeException rejected
                ? (rejected.retryable() ? EmailDeliveryStatus.RETRYABLE : EmailDeliveryStatus.FAILED)
                : EmailDeliveryStatus.UNKNOWN;
            finish(id, claim.attempt(), outcome, null);
            LOG.warn("Email delivery attempt ended: deliveryId={} provider={} status={}", id, provider, outcome);
            throw exception;
        }
        // If this commit fails, SENDING remains durable and a retry cannot blindly send again.
        finish(id, claim.attempt(), EmailDeliveryStatus.ACCEPTED, receipt);
        LOG.info("Email accepted: deliveryId={} provider={} providerMessageId={}", id, provider, receipt);
    }

    private record Claim(EmailDeliveryStatus status, int attempt) { }

    private void finish(final UUID id, final int attempt, final EmailDeliveryStatus outcome, final String receipt) {
        transaction.executeWithoutResult(status -> {
            final var delivery = repository.lockById(id).orElseThrow();
            if (delivery.getAttemptCount() != attempt || delivery.getStatus() != EmailDeliveryStatus.SENDING) {
                throw new EmailDeliveryException(provider, "Email delivery " + id + " requires reconciliation");
            }
            final Instant now = clock.instant();
            delivery.finish(outcome, receipt, now);
            repository.recordOutcome(id, attempt, outcome.name(), receipt, now);
        });
    }
}
