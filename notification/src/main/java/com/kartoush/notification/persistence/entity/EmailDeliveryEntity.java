package com.kartoush.notification.persistence.entity;

import com.kartoush.notification.email.delivery.EmailDeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_delivery")
public class EmailDeliveryEntity {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String provider;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailDeliveryStatus status;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "attempt_started_at", nullable = false)
    private Instant attemptStartedAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "provider_message_id")
    private String providerMessageId;
    @Column(name = "reconciliation_reference")
    private String reconciliationReference;

    protected EmailDeliveryEntity() { }

    public String getProvider() { return provider; }
    public EmailDeliveryStatus getStatus() { return status; }
    public Instant getAttemptStartedAt() { return attemptStartedAt; }
    public int getAttemptCount() { return attemptCount; }

    public void startAttempt(final Instant now) {
        status = EmailDeliveryStatus.SENDING;
        attemptCount++;
        attemptStartedAt = now;
        updatedAt = now;
    }

    public void finish(final EmailDeliveryStatus outcome, final String messageId, final Instant now) {
        status = outcome;
        providerMessageId = messageId;
        updatedAt = now;
    }

    public void reconcile(final EmailDeliveryStatus outcome, final String messageId,
                          final String reference, final Instant now) {
        finish(outcome, messageId, now);
        reconciliationReference = reference;
    }
}
