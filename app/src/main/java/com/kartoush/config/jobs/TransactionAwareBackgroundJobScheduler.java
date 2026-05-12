package com.kartoush.config.jobs;

import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.jobs.JobRequest;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

public class TransactionAwareBackgroundJobScheduler implements BackgroundJobScheduler {

    private final JobRunrPlatformJobScheduler delegate;

    public TransactionAwareBackgroundJobScheduler(final JobRunrPlatformJobScheduler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void enqueue(final JobRequest request) {
        assertTransactionIsActive();
        final JobRunrPlatformJobScheduler.SerializedJobRequest serializedJobRequest = delegate.snapshot(request);
        registerAfterCommit(() -> delegate.enqueueSerialized(serializedJobRequest));
    }

    @Override
    public void schedule(final JobRequest request, final Instant scheduledAt) {
        assertTransactionIsActive();
        final JobRunrPlatformJobScheduler.SerializedJobRequest serializedJobRequest = delegate.snapshot(request);
        registerAfterCommit(() -> delegate.scheduleSerialized(serializedJobRequest, scheduledAt));
    }

    private void registerAfterCommit(final Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void assertTransactionIsActive() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
            || !TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Background jobs can only be scheduled inside an active transaction");
        }
    }
}
