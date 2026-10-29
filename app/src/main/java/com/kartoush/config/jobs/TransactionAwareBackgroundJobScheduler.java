package com.kartoush.config.jobs;

import com.kartoush.jobs.persistence.entity.BackgroundJobOutboxEntity;
import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.jobs.JobRequest;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Objects;

public class TransactionAwareBackgroundJobScheduler implements BackgroundJobScheduler {
    private final JobRunrPlatformJobScheduler serializer;
    private final BackgroundJobOutboxRepository outboxRepository;

    public TransactionAwareBackgroundJobScheduler(final JobRunrPlatformJobScheduler serializer,
        final BackgroundJobOutboxRepository outboxRepository) {
        this.serializer = serializer;
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void enqueue(final JobRequest request) {
        schedule(request, Instant.now());
    }

    @Override
    public void schedule(final JobRequest request, final Instant scheduledAt) {
        assertTransactionIsActive();
        Objects.requireNonNull(scheduledAt, "scheduledAt is required");
        final var snapshot = serializer.snapshot(Objects.requireNonNull(request, "request is required"));
        outboxRepository.save(new BackgroundJobOutboxEntity(snapshot.requestType(), snapshot.payload(), scheduledAt));
    }

    private void assertTransactionIsActive() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
            || !TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Background jobs can only be scheduled inside an active transaction");
        }
    }
}
