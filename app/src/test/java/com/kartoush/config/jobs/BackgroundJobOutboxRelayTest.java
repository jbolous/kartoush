package com.kartoush.config.jobs;

import com.kartoush.jobs.persistence.entity.BackgroundJobOutboxEntity;
import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BackgroundJobOutboxRelayTest {
    @Test
    void shouldLeavePendingIntentWhenEnqueueFailsAndRetryOnNextPoll() {
        final BackgroundJobOutboxRepository repository = mock(BackgroundJobOutboxRepository.class);
        final JobScheduler scheduler = mock(JobScheduler.class);
        final OutboxJobExecutor executor = mock(OutboxJobExecutor.class);
        final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        final BackgroundJobOutboxEntity job = new BackgroundJobOutboxEntity("type", "payload", Instant.now());
        when(repository.lockNextPending()).thenReturn(Optional.of(job), Optional.of(job), Optional.empty());
        when(scheduler.enqueue(any(JobLambda.class))).thenThrow(new IllegalStateException("queue unavailable"))
            .thenReturn(new org.jobrunr.jobs.JobId(java.util.UUID.randomUUID()));
        final BackgroundJobOutboxRelay relay = new BackgroundJobOutboxRelay(repository, scheduler, executor, manager);

        relay.publishPending();
        assertThat(job.getDispatchedAt()).isNull();
        verify(manager).rollback(any());

        relay.publishPending();
        assertThat(job.getDispatchedAt()).isNotNull();
        verify(scheduler, times(2)).enqueue(any(JobLambda.class));
    }
}
