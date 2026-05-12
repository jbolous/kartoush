package com.kartoush.config;

import com.kartoush.config.jobs.TransactionAwareBackgroundJobScheduler;
import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.jobs.JobRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionAwareBackgroundJobSchedulerTest {

    private final RecordingBackgroundJobScheduler delegate = new RecordingBackgroundJobScheduler();

    private final TransactionAwareBackgroundJobScheduler scheduler =
        new TransactionAwareBackgroundJobScheduler(delegate);

    @AfterEach
    void tearDownTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void shouldEnqueueOnlyAfterCommit() {
        beginTransaction();
        final ExampleJobRequest request = new ExampleJobRequest("01JAFTERCOMMIT0000000000001");

        scheduler.enqueue(request);

        assertThat(delegate.enqueuedRequest).isNull();

        triggerAfterCommit();

        assertThat(delegate.enqueuedRequest).isEqualTo(request);
    }

    @Test
    void shouldNotEnqueueWhenTransactionRollsBack() {
        beginTransaction();

        scheduler.enqueue(new ExampleJobRequest("01JAFTERCOMMIT0000000000002"));

        triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        assertThat(delegate.enqueuedRequest).isNull();
    }

    @Test
    void shouldScheduleOnlyAfterCommit() {
        beginTransaction();
        final ExampleJobRequest request = new ExampleJobRequest("01JAFTERCOMMIT0000000000003");
        final Instant scheduledAt = Instant.parse("2026-05-12T12:00:00Z");

        scheduler.schedule(request, scheduledAt);

        assertThat(delegate.scheduledRequest).isNull();
        assertThat(delegate.scheduledAt).isNull();

        triggerAfterCommit();

        assertThat(delegate.scheduledRequest).isEqualTo(request);
        assertThat(delegate.scheduledAt).isEqualTo(scheduledAt);
    }

    @Test
    void shouldRejectSchedulingOutsideActiveTransaction() {
        assertThatThrownBy(() -> scheduler.enqueue(new ExampleJobRequest("01JAFTERCOMMIT0000000000004")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Background jobs can only be scheduled inside an active transaction");
    }

    private void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    private void triggerAfterCommit() {
        final List<TransactionSynchronization> synchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        for (final TransactionSynchronization synchronization : synchronizations) {
            synchronization.afterCommit();
        }

        triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    }

    private void triggerAfterCompletion(final int status) {
        final List<TransactionSynchronization> synchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        for (final TransactionSynchronization synchronization : synchronizations) {
            synchronization.afterCompletion(status);
        }

        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    record ExampleJobRequest(String customerId) implements JobRequest {
    }

    private static final class RecordingBackgroundJobScheduler implements BackgroundJobScheduler {

        private JobRequest enqueuedRequest;

        private JobRequest scheduledRequest;

        private Instant scheduledAt;

        @Override
        public void enqueue(final JobRequest request) {
            this.enqueuedRequest = request;
        }

        @Override
        public void schedule(final JobRequest request, final Instant scheduledAt) {
            this.scheduledRequest = request;
            this.scheduledAt = scheduledAt;
        }
    }
}
