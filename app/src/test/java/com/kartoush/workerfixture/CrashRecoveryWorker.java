package com.kartoush.workerfixture;

import com.kartoush.KartoushApplication;
import com.kartoush.notification.email.EmailMessage;
import com.kartoush.notification.email.EmailMessageType;
import com.kartoush.notification.email.delivery.TrackedEmailDeliveryService;
import com.kartoush.notification.persistence.repository.EmailDeliveryRepository;
import com.kartoush.platform.jobs.JobHandler;
import com.kartoush.platform.jobs.JobRequest;
import com.kartoush.platform.types.Email;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/** Real worker process used only by the crash-recovery integration test. */
public class CrashRecoveryWorker {
    public static void main(final String[] args) {
        final var application = new SpringApplication(KartoushApplication.class, Jobs.class);
        application.run("--spring.profiles.active=test", "--server.port=0",
            "--spring.datasource.url=" + System.getenv("RECOVERY_TEST_DB_URL"),
            "--spring.datasource.username=" + System.getenv("RECOVERY_TEST_DB_USER"),
            "--spring.datasource.password=" + System.getenv("RECOVERY_TEST_DB_PASSWORD"),
            "--jobrunr.background-job-server.enabled=true",
            "--jobrunr.background-job-server.worker-count=1",
            "--jobrunr.background-job-server.poll-interval-in-seconds=5",
            "--jobrunr.background-job-server.server-timeout-poll-interval-multiplicand=4",
            "--logging.level.root=WARN", "--logging.level.com.kartoush=INFO",
            "--logging.level.org.hibernate.SQL=OFF", "--logging.level.org.hibernate.orm.jdbc.bind=OFF",
            "--logging.level.org.springframework.transaction=OFF", "--logging.level.org.springframework.orm.jpa=OFF");
    }

    public record RecoveryRequest(UUID id) implements JobRequest { }

    @TestConfiguration
    public static class Jobs {
        @Bean
        RecoveryHandler recoveryHandler(final JdbcTemplate jdbc, final EmailDeliveryRepository repository,
            final PlatformTransactionManager manager, final Clock clock) {
            final var receiptTransaction = new TransactionTemplate(manager);
            receiptTransaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
            final var delivery = new TrackedEmailDeliveryService("test-provider", email -> {
                // Simulates a provider acceptance committed independently of the worker's business transaction.
                receiptTransaction.executeWithoutResult(status -> jdbc.update(
                    "insert into recovery_test_receipts (id, sends) values (?, 1) "
                    + "on conflict (id) do update set sends = recovery_test_receipts.sends + 1",
                    UUID.fromString(email.idempotencyKey())));
                return Optional.of("test-provider-receipt");
            }, repository, manager, clock);
            return new RecoveryHandler(jdbc, delivery);
        }
    }

    public static class RecoveryHandler implements JobHandler<RecoveryRequest> {
        private final JdbcTemplate jdbc;
        private final TrackedEmailDeliveryService delivery;

        public RecoveryHandler(final JdbcTemplate jdbc, final TrackedEmailDeliveryService delivery) {
            this.jdbc = jdbc;
            this.delivery = delivery;
        }

        @Override
        public void handle(final RecoveryRequest request) {
            delivery.send(new EmailMessage(EmailMessageType.CUSTOMER_ACTIVATION,
                new Email("recipient@example.com"), new Email("sender@example.com"), "Test",
                "Test recovery", "Test message", "https://example.com/activate")
                .withIdempotencyKey(request.id().toString()));
            jdbc.update("insert into recovery_test_business (id) values (?)", request.id());
            if ("true".equals(System.getenv("RECOVERY_TEST_CRASH"))) {
                try {
                    Files.writeString(Path.of(System.getenv("RECOVERY_TEST_READY")), "inside-handler");
                    new CountDownLatch(1).await();
                } catch (final Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }
        }
    }
}
