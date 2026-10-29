package com.kartoush.config.jobs;

import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.testsupport.PostgresSpringIntegrationTest;
import com.kartoush.testsupport.SpringIntegrationTest;
import com.kartoush.workerfixture.CrashRecoveryWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringIntegrationTest
@Import(CrashRecoveryWorker.Jobs.class)
class BackgroundWorkerCrashIntegrationTest extends PostgresSpringIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BackgroundJobScheduler scheduler;
    @Autowired private BackgroundJobOutboxRepository outbox;
    @Autowired private PlatformTransactionManager manager;
    @TempDir private Path directory;

    @Test
    void shouldRecoverOnAnotherWorkerWithoutResendingWhenProcessDiesMidHandler() throws Exception {
        jdbc.execute("create table if not exists recovery_test_receipts (id uuid primary key, sends integer not null)");
        jdbc.execute("create table if not exists recovery_test_business (id uuid primary key)");
        final var id = UUID.randomUUID();
        new TransactionTemplate(manager).executeWithoutResult(status ->
            scheduler.enqueue(new CrashRecoveryWorker.RecoveryRequest(id)));
        final var intent = outbox.findAll().stream()
            .filter(job -> job.getPayload() != null && job.getPayload().contains(id.toString()))
            .findFirst().orElseThrow();
        final var ready = directory.resolve("worker-ready");
        Process first = null;
        Process replacement = null;
        try {
            first = worker(true, ready, "crashed-worker.log");
            final Process running = first;
            await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
                assertThat(running.isAlive()).withFailMessage(() -> log("crashed-worker.log")).isTrue();
                assertThat(Files.exists(ready)).withFailMessage(() -> log("crashed-worker.log")).isTrue();
            });
            assertThat(jdbc.queryForObject("select status from email_delivery where id = ?", String.class, id))
                .isEqualTo("ACCEPTED");
            assertThat(jdbc.queryForObject("select count(*) from recovery_test_business where id = ?", Integer.class, id))
                .isZero();
            first.destroyForcibly();
            assertThat(first.waitFor(10, TimeUnit.SECONDS)).isTrue();
            assertThat(outbox.findById(intent.getId()).orElseThrow().getCompletedAt()).isNull();

            replacement = worker(false, ready, "replacement-worker.log");
            final Process survivor = replacement;
            await().atMost(Duration.ofSeconds(90)).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {
                assertThat(survivor.isAlive()).withFailMessage(() -> log("replacement-worker.log")).isTrue();
                assertThat(outbox.findById(intent.getId()).orElseThrow().getCompletedAt()).isNotNull();
            });
            assertThat(jdbc.queryForObject("select count(*) from recovery_test_business where id = ?", Integer.class, id))
                .isEqualTo(1);
            assertThat(jdbc.queryForObject("select sends from recovery_test_receipts where id = ?", Integer.class, id))
                .isEqualTo(1);
        } finally {
            for (final Process process : new Process[] {first, replacement}) {
                if (process != null && process.isAlive()) {
                    process.destroyForcibly();
                    process.waitFor(10, TimeUnit.SECONDS);
                }
            }
        }
    }

    private Process worker(final boolean crash, final Path ready, final String log) throws Exception {
        final var builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", System.getProperty("kartoush.test.runtime-classpath"), CrashRecoveryWorker.class.getName());
        builder.environment().put("RECOVERY_TEST_DB_URL", POSTGRES.getJdbcUrl());
        builder.environment().put("RECOVERY_TEST_DB_USER", POSTGRES.getUsername());
        builder.environment().put("RECOVERY_TEST_DB_PASSWORD", POSTGRES.getPassword());
        builder.environment().put("RECOVERY_TEST_CRASH", Boolean.toString(crash));
        builder.environment().put("RECOVERY_TEST_READY", ready.toString());
        return builder.redirectErrorStream(true).redirectOutput(directory.resolve(log).toFile()).start();
    }

    private String log(final String name) {
        try { return Files.readString(directory.resolve(name)); }
        catch (final Exception exception) { return exception.toString(); }
    }
}
