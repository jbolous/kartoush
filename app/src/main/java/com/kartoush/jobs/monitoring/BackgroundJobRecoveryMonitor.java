package com.kartoush.jobs.monitoring;

import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import com.kartoush.notification.email.delivery.EmailDeliveryRecoveryService;
import com.kartoush.notification.email.delivery.EmailDeliveryStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class BackgroundJobRecoveryMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(BackgroundJobRecoveryMonitor.class);
    private final EmailDeliveryRecoveryService deliveries;
    private final BackgroundJobOutboxRepository outbox;
    private final StorageProvider storage;
    private final Clock clock;
    private final Counter failures;
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>();

    public BackgroundJobRecoveryMonitor(final EmailDeliveryRecoveryService deliveries,
        final BackgroundJobOutboxRepository outbox, final StorageProvider storage,
        final Clock clock, final MeterRegistry registry) {
        this.deliveries = deliveries;
        this.outbox = outbox;
        this.storage = storage;
        this.clock = clock;
        failures = registry.counter("jobs.recovery.monitor.failures");
        for (final String kind : new String[] {"email-unknown", "email-failed", "outbox-quarantined", "execution-failed"}) {
            Gauge.builder("jobs.recovery.unresolved", this, monitor -> monitor.count(kind))
                .tag("kind", kind).register(registry);
            Gauge.builder("jobs.recovery.oldest.age", this, monitor -> monitor.oldestAge(kind))
                .tag("kind", kind).baseUnit("seconds").register(registry);
        }
        Gauge.builder("jobs.recovery.snapshot.age", this, monitor -> {
            final var current = monitor.snapshot.get();
            return current == null ? Double.NaN : monitor.age(current.at());
        }).baseUnit("seconds").register(registry);
    }

    @Scheduled(fixedDelayString = "${kartoush.jobs.recovery.poll-interval-ms:60000}")
    public void check() {
        try {
            deliveries.markStaleAttemptsUnknown();
            final var unknown = deliveries.attention(EmailDeliveryStatus.UNKNOWN);
            final var failed = deliveries.attention(EmailDeliveryStatus.FAILED);
            final long quarantined = outbox.countByQuarantinedAtIsNotNullAndCompletedAtIsNull();
            final var quarantineAt = outbox.oldestQuarantinedAt().orElse(null);
            final long failedJobs = storage.countJobs(StateName.FAILED);
            final var oldestFailed = storage.getJobList(StateName.FAILED, new AmountRequest("updatedAt:ASC", 1))
                .stream().findFirst().map(job -> job.getUpdatedAt()).orElse(null);
            snapshot.set(new Snapshot(clock.instant(), unknown, failed,
                new EmailDeliveryRecoveryService.Attention(quarantined, quarantineAt),
                new EmailDeliveryRecoveryService.Attention(failedJobs, oldestFailed)));
            if (unknown.count() + failed.count() + quarantined + failedJobs > 0) {
                LOG.warn("Background jobs require attention: unknownEmails={} failedEmails={} quarantinedOutbox={} failedExecutions={}",
                    unknown.count(), failed.count(), quarantined, failedJobs);
            }
        } catch (final RuntimeException exception) {
            failures.increment();
            LOG.error("Background job recovery monitor failed; its metrics snapshot is stale", exception);
        }
    }

    private EmailDeliveryRecoveryService.Attention attention(final Snapshot current, final String kind) {
        return switch (kind) {
            case "email-unknown" -> current.unknown();
            case "email-failed" -> current.failed();
            case "outbox-quarantined" -> current.quarantined();
            case "execution-failed" -> current.executions();
            default -> throw new IllegalArgumentException(kind);
        };
    }

    private double count(final String kind) {
        final var current = snapshot.get();
        return current == null ? Double.NaN : attention(current, kind).count();
    }

    private double oldestAge(final String kind) {
        final var current = snapshot.get();
        return current == null ? Double.NaN : age(attention(current, kind).oldestAt());
    }

    private double age(final Instant at) {
        return at == null ? 0 : Math.max(0, Duration.between(at, clock.instant()).toMillis() / 1000.0);
    }

    private record Snapshot(Instant at, EmailDeliveryRecoveryService.Attention unknown,
        EmailDeliveryRecoveryService.Attention failed, EmailDeliveryRecoveryService.Attention quarantined,
        EmailDeliveryRecoveryService.Attention executions) { }
}
