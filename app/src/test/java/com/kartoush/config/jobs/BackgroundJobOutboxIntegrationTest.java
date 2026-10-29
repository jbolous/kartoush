package com.kartoush.config.jobs;

import com.kartoush.jobs.persistence.entity.BackgroundJobOutboxEntity;
import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.jobs.JobHandler;
import com.kartoush.platform.jobs.JobRequest;
import com.kartoush.testsupport.PostgresSpringIntegrationTest;
import com.kartoush.testsupport.SpringIntegrationTest;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringIntegrationTest
@Import(BackgroundJobOutboxIntegrationTest.TestJobs.class)
class BackgroundJobOutboxIntegrationTest extends PostgresSpringIntegrationTest {
    @Autowired private BackgroundJobScheduler scheduler;
    @Autowired private BackgroundJobOutboxRepository repository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JobScheduler jobScheduler;
    @Autowired private OutboxJobExecutor executor;
    @Autowired private CountingHandler handler;

    @Test
    void shouldRollBackOutboxWithBusinessTransaction() {
        final long before = repository.count();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            scheduler.enqueue(new TestRequest("rollback"));
            status.setRollbackOnly();
        });

        assertThat(repository.count()).isEqualTo(before);
    }

    @Test
    void shouldRecoverCommittedIntentAndSuppressDuplicateExecution() {
        final String marker = UUID.randomUUID().toString();
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            scheduler.schedule(new TestRequest(marker), Instant.EPOCH));
        final BackgroundJobOutboxEntity job = repository.findAll().stream()
            .filter(record -> record.getPayload() != null && record.getPayload().contains(marker))
            .findFirst().orElseThrow();
        assertThat(job.getDispatchedAt()).isNull();

        new BackgroundJobOutboxRelay(repository, jobScheduler, executor, transactionManager,
            new BackgroundJobOutboxProperties(5, Duration.ofSeconds(30), Duration.ofHours(1)), Clock.systemUTC()).publishPending();

        assertThat(repository.findById(job.getId()).orElseThrow().getDispatchedAt()).isNotNull();
        final int before = handler.executions.get();
        executor.execute(job.getId());
        executor.execute(job.getId());

        assertThat(handler.executions.get()).isEqualTo(before + 1);
        final BackgroundJobOutboxEntity completed = repository.findById(job.getId()).orElseThrow();
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getPayload()).isNull();
    }

    @Test
    void shouldSerializeConcurrentExecutionsOfTheSameIntent() throws Exception {
        final BackgroundJobOutboxEntity job = new TransactionTemplate(transactionManager).execute(status ->
            repository.save(new BackgroundJobOutboxEntity(TestRequest.class.getName(),
                "{\"marker\":\"concurrent\"}", Instant.now())));
        final int before = handler.executions.get();

        CompletableFuture.allOf(
            CompletableFuture.runAsync(() -> executor.execute(job.getId())),
            CompletableFuture.runAsync(() -> executor.execute(job.getId()))
        ).get(10, TimeUnit.SECONDS);

        assertThat(handler.executions.get()).isEqualTo(before + 1);
    }

    @Test
    void shouldLeaveFutureJobsPendingUntilDue() {
        final String marker = UUID.randomUUID().toString();
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            scheduler.schedule(new TestRequest(marker), Instant.now().plusSeconds(3600)));

        new BackgroundJobOutboxRelay(repository, jobScheduler, executor, transactionManager,
            new BackgroundJobOutboxProperties(5, Duration.ofSeconds(30), Duration.ofHours(1)), Clock.systemUTC()).publishPending();

        final BackgroundJobOutboxEntity job = repository.findAll().stream()
            .filter(record -> record.getPayload() != null && record.getPayload().contains(marker))
            .findFirst().orElseThrow();
        assertThat(job.getDispatchedAt()).isNull();
    }

    public record TestRequest(String marker) implements JobRequest { }

    static class CountingHandler implements JobHandler<TestRequest> {
        final AtomicInteger executions = new AtomicInteger();
        public void handle(final TestRequest request) { executions.incrementAndGet(); }
    }

    @TestConfiguration
    static class TestJobs {
        @Bean CountingHandler countingHandler() { return new CountingHandler(); }
    }
}
