package com.kartoush.config.jobs;

import com.kartoush.jobs.persistence.entity.BackgroundJobOutboxEntity;
import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class OutboxJobExecutorTest {
    private final BackgroundJobOutboxRepository repository = mock(BackgroundJobOutboxRepository.class);
    private final PlatformJobDispatcher dispatcher = mock(PlatformJobDispatcher.class);
    private final OutboxJobExecutor executor = new OutboxJobExecutor(repository, dispatcher);

    @Test
    void shouldSkipDuplicateExecutionAndClearCompletedPayload() {
        final BackgroundJobOutboxEntity job = new BackgroundJobOutboxEntity("type", "encrypted-payload", Instant.now());
        when(repository.lockById(job.getId())).thenReturn(Optional.of(job));

        executor.execute(job.getId());
        executor.execute(job.getId());

        verify(dispatcher, times(1)).dispatch("type", "encrypted-payload");
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.getPayload()).isNull();
    }

    @Test
    void shouldLeaveFailedExecutionIncompleteForRetry() {
        final BackgroundJobOutboxEntity job = new BackgroundJobOutboxEntity("type", "payload", Instant.now());
        when(repository.lockById(job.getId())).thenReturn(Optional.of(job));
        doThrow(new IllegalStateException("provider unavailable")).when(dispatcher).dispatch("type", "payload");

        assertThatThrownBy(() -> executor.execute(job.getId())).isInstanceOf(IllegalStateException.class);

        assertThat(job.getCompletedAt()).isNull();
        assertThat(job.getPayload()).isEqualTo("payload");
    }
}
