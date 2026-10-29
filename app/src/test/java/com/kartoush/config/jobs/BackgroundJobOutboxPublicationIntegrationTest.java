package com.kartoush.config.jobs;

import com.kartoush.jobs.persistence.entity.BackgroundJobOutboxEntity;
import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import com.kartoush.jobs.service.BackgroundJobOutboxRecoveryService;
import com.kartoush.testsupport.PostgresSpringIntegrationTest;
import com.kartoush.testsupport.SpringIntegrationTest;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringIntegrationTest
class BackgroundJobOutboxPublicationIntegrationTest extends PostgresSpringIntegrationTest {
    @Autowired private BackgroundJobOutboxRepository repository;
    @Autowired private PlatformTransactionManager manager;
    @Autowired private OutboxJobExecutor executor;
    @Autowired private BackgroundJobOutboxRecoveryService recovery;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void clearPendingIntents() { repository.deleteAll(); }

    @Test
    void shouldPublishLaterIntentWhenEarliestPublicationFails() {
        final var earlier = save(Instant.EPOCH.minusSeconds(1));
        final var failed = save(Instant.EPOCH);
        final var later = save(Instant.EPOCH.plusSeconds(1));
        final var scheduler = mock(JobScheduler.class);
        when(scheduler.enqueue(any(JobLambda.class))).thenReturn(new JobId(UUID.randomUUID()))
            .thenThrow(new IllegalStateException("unpublishable"))
            .thenAnswer(invocation -> new JobId(UUID.randomUUID()));
        final var relay = relay(scheduler, 5);

        relay.publishPending();

        assertThat(repository.findById(earlier.getId()).orElseThrow().getDispatchedAt()).isNotNull();
        final var reloaded = repository.findById(failed.getId()).orElseThrow();
        assertThat(reloaded.getDispatchedAt()).isNull();
        assertThat(reloaded.getPublicationFailures()).isEqualTo(1);
        assertThat(reloaded.getNextPublicationAt()).isAfter(Instant.now());
        assertThat(reloaded.getQuarantinedAt()).isNull();
        assertThat(repository.findById(later.getId()).orElseThrow().getDispatchedAt()).isNotNull();
        clearInvocations(scheduler);
        relay.publishPending();
        verifyNoInteractions(scheduler);
    }

    @Test
    void shouldQuarantineRepeatedFailureAndAllowExplicitRecoveryWhenCauseIsResolved() {
        final var failed = save(Instant.EPOCH.minusSeconds(1));
        final var scheduler = mock(JobScheduler.class);
        when(scheduler.enqueue(any(JobLambda.class))).thenThrow(new IllegalStateException("unpublishable"));
        final var relay = relay(scheduler, 2);
        relay.publishPending();
        assertThat(repository.findById(failed.getId()).orElseThrow().getPublicationFailures()).isEqualTo(1);
        jdbc.update("update background_job_outbox set next_publication_at = current_timestamp - interval '1 second' where id = ?",
            failed.getId());
        relay.publishPending();
        final var quarantined = repository.findById(failed.getId()).orElseThrow();
        assertThat(quarantined.getPublicationFailures()).isEqualTo(2);
        assertThat(quarantined.getQuarantinedAt()).isNotNull();
        assertThat(quarantined.getNextPublicationAt()).isNull();
        clearInvocations(scheduler);
        relay.publishPending();
        verifyNoInteractions(scheduler);

        recovery.retryPublication(failed.getId());
        doReturn(new JobId(UUID.randomUUID())).when(scheduler).enqueue(any(JobLambda.class));
        relay.publishPending();
        assertThat(repository.findById(failed.getId()).orElseThrow().getDispatchedAt()).isNotNull();
        assertThatThrownBy(() -> recovery.retryPublication(failed.getId())).isInstanceOf(IllegalStateException.class);
    }

    private BackgroundJobOutboxEntity save(final Instant availableAt) {
        return new TransactionTemplate(manager).execute(status -> repository.save(
            new BackgroundJobOutboxEntity("test-publication", "{}", availableAt)));
    }

    private BackgroundJobOutboxRelay relay(final JobScheduler scheduler, final int threshold) {
        return new BackgroundJobOutboxRelay(repository, scheduler, executor, manager,
            new BackgroundJobOutboxProperties(threshold, Duration.ofSeconds(30), Duration.ofHours(1)), Clock.systemUTC());
    }
}
