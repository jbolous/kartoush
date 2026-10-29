package com.kartoush.config.jobs;

import com.kartoush.jobs.persistence.entity.BackgroundJobOutboxEntity;
import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import com.kartoush.platform.jobs.JobRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TransactionAwareBackgroundJobSchedulerTest {
    private final JobRunrPlatformJobScheduler serializer = mock(JobRunrPlatformJobScheduler.class);
    private final BackgroundJobOutboxRepository repository = mock(BackgroundJobOutboxRepository.class);
    private final TransactionAwareBackgroundJobScheduler scheduler =
        new TransactionAwareBackgroundJobScheduler(serializer, repository);

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void shouldPersistSnapshotInCurrentTransactionWithoutCallingJobRunr() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        final ExampleJobRequest request = new ExampleJobRequest("customer");
        final Instant scheduledAt = Instant.parse("2030-01-01T00:00:00Z");
        when(serializer.snapshot(request)).thenReturn(
            new JobRunrPlatformJobScheduler.SerializedJobRequest(ExampleJobRequest.class.getName(), "payload"));

        scheduler.schedule(request, scheduledAt);

        final ArgumentCaptor<BackgroundJobOutboxEntity> captor = ArgumentCaptor.forClass(BackgroundJobOutboxEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo("payload");
        assertThat(captor.getValue().getAvailableAt()).isEqualTo(scheduledAt);
        assertThat(captor.getValue().getDispatchedAt()).isNull();
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
        verify(serializer).snapshot(request);
        verifyNoMoreInteractions(serializer);
    }

    @Test
    void shouldRejectSchedulingOutsideTransaction() {
        assertThatThrownBy(() -> scheduler.enqueue(new ExampleJobRequest("customer")))
            .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(repository, serializer);
    }

    record ExampleJobRequest(String customerId) implements JobRequest { }
}
