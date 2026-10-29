package com.kartoush.jobs.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "background_job_outbox")
public class BackgroundJobOutboxEntity {
    @Id
    private UUID id;
    @Column(name = "request_type", nullable = false, length = 512)
    private String requestType;
    @Column(columnDefinition = "text")
    private String payload;
    @Column(name = "available_at", nullable = false)
    private Instant availableAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "dispatched_at")
    private Instant dispatchedAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "publication_failures", nullable = false)
    private int publicationFailures;
    @Column(name = "next_publication_at")
    private Instant nextPublicationAt;
    @Column(name = "quarantined_at")
    private Instant quarantinedAt;

    protected BackgroundJobOutboxEntity() {
    }

    public BackgroundJobOutboxEntity(final String requestType, final String payload, final Instant availableAt) {
        this.id = UUID.randomUUID();
        this.requestType = requestType;
        this.payload = payload;
        this.availableAt = availableAt;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getRequestType() { return requestType; }
    public String getPayload() { return payload; }
    public Instant getAvailableAt() { return availableAt; }
    public Instant getDispatchedAt() { return dispatchedAt; }
    public Instant getCompletedAt() { return completedAt; }

    public int getPublicationFailures() { return publicationFailures; }
    public Instant getNextPublicationAt() { return nextPublicationAt; }
    public Instant getQuarantinedAt() { return quarantinedAt; }

    public void recordPublicationFailure(final Instant now, final Instant retryAt, final int maxFailures) {
        publicationFailures++;
        if (publicationFailures >= maxFailures) {
            quarantinedAt = now;
            nextPublicationAt = null;
        } else {
            nextPublicationAt = retryAt;
        }
    }

    public void retryPublication() {
        if (quarantinedAt == null || dispatchedAt != null || completedAt != null) {
            throw new IllegalStateException("Only quarantined unpublished jobs can be retried");
        }
        publicationFailures = 0;
        nextPublicationAt = null;
        quarantinedAt = null;
    }

    public void markDispatched() { dispatchedAt = Instant.now(); }

    public void markCompleted() {
        completedAt = Instant.now();
        payload = null;
    }
}
