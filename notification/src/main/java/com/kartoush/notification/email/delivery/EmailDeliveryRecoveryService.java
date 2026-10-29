package com.kartoush.notification.email.delivery;

import com.kartoush.notification.persistence.repository.EmailDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class EmailDeliveryRecoveryService {
    public static final Duration STALE_AFTER = Duration.ofMinutes(5);
    private static final Logger LOG = LoggerFactory.getLogger(EmailDeliveryRecoveryService.class);
    private final EmailDeliveryRepository repository;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public EmailDeliveryRecoveryService(final EmailDeliveryRepository repository,
        final PlatformTransactionManager manager, final Clock clock) {
        this.repository = repository;
        this.clock = clock;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    /** Bounded per-row transactions; stale does not mean that a resend is safe. */
    public int markStaleAttemptsUnknown() {
        int changed = 0;
        for (int index = 0; index < 100; index++) {
            final Boolean marked = transaction.execute(status -> {
                final Instant now = clock.instant();
                final var next = repository.lockNextStale(now.minus(STALE_AFTER));
                if (next.isEmpty()) {
                    return false;
                }
                final var delivery = next.orElseThrow();
                delivery.finish(EmailDeliveryStatus.UNKNOWN, null, now);
                repository.recordOutcome(delivery.getId(), delivery.getAttemptCount(), "UNKNOWN", null, now);
                LOG.warn("Stale email delivery requires reconciliation: deliveryId={}", delivery.getId());
                return true;
            });
            if (!Boolean.TRUE.equals(marked)) {
                break;
            }
            changed++;
        }
        return changed;
    }

    public Attention attention(final EmailDeliveryStatus status) {
        return transaction.execute(transactionStatus ->
            new Attention(repository.countByStatus(status), repository.oldestUpdatedAt(status).orElse(null)));
    }

    public record Attention(long count, Instant oldestAt) { }
}
