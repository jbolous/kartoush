package com.kartoush.config.jobs;

import com.kartoush.jobs.persistence.entity.BackgroundJobOutboxEntity;
import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;

public class BackgroundJobOutboxRelay {
    private static final Logger LOG = LoggerFactory.getLogger(BackgroundJobOutboxRelay.class);
    private final BackgroundJobOutboxRepository outboxRepository;
    private final JobScheduler jobScheduler;
    private final OutboxJobExecutor executor;
    private final TransactionTemplate transaction;
    private final BackgroundJobOutboxProperties properties;
    private final Clock clock;

    public BackgroundJobOutboxRelay(final BackgroundJobOutboxRepository outboxRepository,
        final JobScheduler jobScheduler, final OutboxJobExecutor executor,
        final PlatformTransactionManager transactionManager, final BackgroundJobOutboxProperties properties,
        final Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.outboxRepository = outboxRepository;
        this.jobScheduler = jobScheduler;
        this.executor = executor;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedDelayString = "${kartoush.jobs.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        for (int index = 0; index < 100; index++) {
            try {
                final Boolean published = transaction.execute(status -> {
                    final var pending = outboxRepository.lockNextPending();
                    if (pending.isEmpty()) {
                        return false;
                    }
                    final BackgroundJobOutboxEntity job = pending.orElseThrow();
                    final var id = job.getId();
                    try {
                        jobScheduler.enqueue(() -> executor.execute(id));
                        job.markDispatched();
                    } catch (final RuntimeException exception) {
                        final var now = clock.instant();
                        job.recordPublicationFailure(now, now.plus(properties.backoffAfter(job.getPublicationFailures())),
                            properties.maxPublicationFailures());
                        LOG.warn("Background job publication failed: outboxId={} failures={} quarantined={}",
                            id, job.getPublicationFailures(), job.getQuarantinedAt() != null);
                    }
                    return true;
                });
                if (!Boolean.TRUE.equals(published)) {
                    return;
                }
            } catch (final RuntimeException ex) {
                LOG.error("Could not commit outbox publication state; stopping this poll", ex);
                return;
            }
        }
    }
}
