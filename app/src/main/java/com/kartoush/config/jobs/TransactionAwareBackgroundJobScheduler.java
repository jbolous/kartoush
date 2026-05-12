package com.kartoush.config.jobs;

import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.jobs.JobRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

public class TransactionAwareBackgroundJobScheduler implements BackgroundJobScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionAwareBackgroundJobScheduler.class);

    private final JobRunrPlatformJobScheduler delegate;

    private final TransactionOperations transactionOperations;

    public TransactionAwareBackgroundJobScheduler(
        final JobRunrPlatformJobScheduler delegate,
        final TransactionOperations transactionOperations) {
        this.delegate = delegate;
        this.transactionOperations = transactionOperations;
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
                try {
                    transactionOperations.executeWithoutResult(status -> action.run());
                } catch (final RuntimeException ex) {
                    LOG.error("Background job scheduling failed after the original transaction committed", ex);
                }
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
