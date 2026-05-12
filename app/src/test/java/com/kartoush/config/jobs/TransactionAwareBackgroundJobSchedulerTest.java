package com.kartoush.config.jobs;

import com.kartoush.platform.jobs.JobRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionAwareBackgroundJobSchedulerTest {

    private final JobRunrPlatformJobScheduler delegate = mock(JobRunrPlatformJobScheduler.class);

    private final TransactionOperations transactionOperations = mock(TransactionOperations.class);

    private final TransactionAwareBackgroundJobScheduler scheduler =
        new TransactionAwareBackgroundJobScheduler(delegate, transactionOperations);

    TransactionAwareBackgroundJobSchedulerTest() {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            final java.util.function.Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionOperations).executeWithoutResult(any());
    }

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
        final JobRunrPlatformJobScheduler.SerializedJobRequest serializedJobRequest =
            new JobRunrPlatformJobScheduler.SerializedJobRequest(
                ExampleJobRequest.class.getName(),
                "{\"customerId\":\"01JAFTERCOMMIT0000000000001\"}"
            );

        when(delegate.snapshot(request)).thenReturn(serializedJobRequest);

        scheduler.enqueue(request);

        verify(delegate).snapshot(request);
        verify(delegate, never()).enqueueSerialized(serializedJobRequest);

        triggerAfterCommit();

        verify(transactionOperations).executeWithoutResult(any());
        verify(delegate).enqueueSerialized(serializedJobRequest);
    }

    @Test
    void shouldNotEnqueueWhenTransactionRollsBack() {
        beginTransaction();
        final ExampleJobRequest request = new ExampleJobRequest("01JAFTERCOMMIT0000000000002");
        final JobRunrPlatformJobScheduler.SerializedJobRequest serializedJobRequest =
            new JobRunrPlatformJobScheduler.SerializedJobRequest(
                ExampleJobRequest.class.getName(),
                "{\"customerId\":\"01JAFTERCOMMIT0000000000002\"}"
            );

        when(delegate.snapshot(request)).thenReturn(serializedJobRequest);

        scheduler.enqueue(request);

        triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(delegate, never()).enqueueSerialized(serializedJobRequest);
    }

    @Test
    void shouldScheduleOnlyAfterCommit() {
        beginTransaction();
        final ExampleJobRequest request = new ExampleJobRequest("01JAFTERCOMMIT0000000000003");
        final Instant scheduledAt = Instant.parse("2026-05-12T12:00:00Z");
        final JobRunrPlatformJobScheduler.SerializedJobRequest serializedJobRequest =
            new JobRunrPlatformJobScheduler.SerializedJobRequest(
                ExampleJobRequest.class.getName(),
                "{\"customerId\":\"01JAFTERCOMMIT0000000000003\"}"
            );

        when(delegate.snapshot(request)).thenReturn(serializedJobRequest);

        scheduler.schedule(request, scheduledAt);

        verify(delegate).snapshot(request);
        verify(delegate, never()).scheduleSerialized(serializedJobRequest, scheduledAt);

        triggerAfterCommit();

        verify(transactionOperations).executeWithoutResult(any());
        verify(delegate).scheduleSerialized(serializedJobRequest, scheduledAt);
    }

    @Test
    void shouldCaptureSerializedRequestBeforeCommit() {
        beginTransaction();
        final MutableExampleJobRequest request = new MutableExampleJobRequest("before-commit");
        final JobRunrPlatformJobScheduler.SerializedJobRequest serializedJobRequest =
            new JobRunrPlatformJobScheduler.SerializedJobRequest(
                MutableExampleJobRequest.class.getName(),
                "{\"customerId\":\"before-commit\"}"
            );

        when(delegate.snapshot(request)).thenReturn(serializedJobRequest);

        scheduler.enqueue(request);
        request.setCustomerId("after-commit");

        triggerAfterCommit();

        verify(delegate).enqueueSerialized(serializedJobRequest);
    }

    @Test
    void shouldNotPropagateSchedulingFailureAfterCommit() {
        beginTransaction();
        final ExampleJobRequest request = new ExampleJobRequest("01JAFTERCOMMIT0000000000005");
        final JobRunrPlatformJobScheduler.SerializedJobRequest serializedJobRequest =
            new JobRunrPlatformJobScheduler.SerializedJobRequest(
                ExampleJobRequest.class.getName(),
                "{\"customerId\":\"01JAFTERCOMMIT0000000000005\"}"
            );

        when(delegate.snapshot(request)).thenReturn(serializedJobRequest);
        doAnswer(invocation -> {
            throw new RuntimeException("boom");
        }).when(delegate).enqueueSerialized(serializedJobRequest);

        scheduler.enqueue(request);

        triggerAfterCommit();
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

    private static final class MutableExampleJobRequest implements JobRequest {

        private String customerId;

        private MutableExampleJobRequest(final String customerId) {
            this.customerId = customerId;
        }

        public String customerId() {
            return customerId;
        }

        private void setCustomerId(final String customerId) {
            this.customerId = customerId;
        }
    }
}
