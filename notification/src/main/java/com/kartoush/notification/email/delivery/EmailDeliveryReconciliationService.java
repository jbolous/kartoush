package com.kartoush.notification.email.delivery;

import com.kartoush.notification.persistence.repository.EmailDeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.UUID;

@Service
public class EmailDeliveryReconciliationService {
    private final EmailDeliveryRepository repository;
    private final Clock clock;

    public EmailDeliveryReconciliationService(final EmailDeliveryRepository repository, final Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Call only after external evidence confirms acceptance or non-acceptance; never infer from a missing receipt. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reconcile(final UUID id, final boolean accepted, final String providerMessageId,
                          final String evidenceReference) {
        if (evidenceReference == null || evidenceReference.isBlank()) {
            throw new IllegalArgumentException("Reconciliation evidence reference is required");
        }
        final var delivery = repository.lockById(id).orElseThrow();
        final var now = clock.instant();
        if (delivery.getStatus() == EmailDeliveryStatus.SENDING
            && !delivery.getAttemptStartedAt().plus(EmailDeliveryRecoveryService.STALE_AFTER).isAfter(now)) {
            delivery.finish(EmailDeliveryStatus.UNKNOWN, null, now);
        }
        if (delivery.getStatus() != EmailDeliveryStatus.UNKNOWN) {
            throw new IllegalStateException("Only uncertain deliveries can be reconciled");
        }
        delivery.reconcile(accepted ? EmailDeliveryStatus.ACCEPTED : EmailDeliveryStatus.RETRYABLE,
            providerMessageId, evidenceReference, now);
        repository.recordOutcome(id, delivery.getAttemptCount(), accepted ? "ACCEPTED" : "RETRYABLE",
            providerMessageId, now);
    }
}
