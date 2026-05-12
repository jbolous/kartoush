package com.kartoush.config.jobs;

import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.jobs.JobRequest;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

public class TransactionAwareBackgroundJobScheduler implements BackgroundJobScheduler {

    private final BackgroundJobScheduler delegate;

    public TransactionAwareBackgroundJobScheduler(final BackgroundJobScheduler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void enqueue(final JobRequest request) {
        assertTransactionIsActive();
        registerAfterCommit(() -> delegate.enqueue(request));
    }

    @Override
    public void schedule(final JobRequest request, final Instant scheduledAt) {
        assertTransactionIsActive();
        registerAfterCommit(() -> delegate.schedule(request, scheduledAt));
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
