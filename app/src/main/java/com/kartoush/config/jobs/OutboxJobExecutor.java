package com.kartoush.config.jobs;

import com.kartoush.jobs.persistence.entity.BackgroundJobOutboxEntity;
import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class OutboxJobExecutor {
    private final BackgroundJobOutboxRepository outboxRepository;
    private final PlatformJobDispatcher dispatcher;

    public OutboxJobExecutor(final BackgroundJobOutboxRepository outboxRepository,
                             final PlatformJobDispatcher dispatcher) {
        this.outboxRepository = outboxRepository;
        this.dispatcher = dispatcher;
    }

    @Transactional
    public void execute(final UUID id) {
        final BackgroundJobOutboxEntity job = outboxRepository.lockById(id)
            .orElseThrow(() -> new IllegalStateException("Background job outbox record not found: " + id));
        if (job.getCompletedAt() != null) {
            return;
        }
        dispatcher.dispatch(job.getRequestType(), job.getPayload());
        job.markCompleted();
    }
}
