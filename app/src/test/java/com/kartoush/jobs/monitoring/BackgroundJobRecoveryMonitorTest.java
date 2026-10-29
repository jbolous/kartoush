package com.kartoush.jobs.monitoring;

import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import com.kartoush.notification.email.delivery.EmailDeliveryRecoveryService;
import com.kartoush.notification.email.delivery.EmailDeliveryStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BackgroundJobRecoveryMonitorTest {
    @Test
    void shouldReportUnresolvedWorkAndKeepSnapshotWhenRefreshFails() {
        final var now = Instant.parse("2026-10-29T00:00:00Z");
        final var deliveries = mock(EmailDeliveryRecoveryService.class);
        final var outbox = mock(BackgroundJobOutboxRepository.class);
        final var storage = mock(StorageProvider.class);
        final var failedJob = mock(Job.class);
        when(failedJob.getUpdatedAt()).thenReturn(now.minusSeconds(60));
        when(deliveries.attention(EmailDeliveryStatus.UNKNOWN))
            .thenReturn(new EmailDeliveryRecoveryService.Attention(2, now.minusSeconds(120)));
        when(deliveries.attention(EmailDeliveryStatus.FAILED))
            .thenReturn(new EmailDeliveryRecoveryService.Attention(1, now.minusSeconds(30)));
        when(outbox.countByQuarantinedAtIsNotNullAndCompletedAtIsNull()).thenReturn(3L);
        when(outbox.oldestQuarantinedAt()).thenReturn(Optional.of(now.minusSeconds(45)));
        when(storage.countJobs(StateName.FAILED)).thenReturn(4L);
        when(storage.getJobList(eq(StateName.FAILED), any(AmountRequest.class))).thenReturn(List.of(failedJob));
        final var registry = new SimpleMeterRegistry();
        final var monitor = new BackgroundJobRecoveryMonitor(deliveries, outbox, storage,
            Clock.fixed(now, ZoneOffset.UTC), registry);
        assertThat(registry.get("jobs.recovery.snapshot.age").gauge().value()).isNaN();

        monitor.check();

        assertThat(registry.get("jobs.recovery.unresolved").tag("kind", "email-unknown").gauge().value()).isEqualTo(2);
        assertThat(registry.get("jobs.recovery.oldest.age").tag("kind", "email-unknown").gauge().value()).isEqualTo(120);
        assertThat(registry.get("jobs.recovery.unresolved").tag("kind", "outbox-quarantined").gauge().value()).isEqualTo(3);
        assertThat(registry.get("jobs.recovery.unresolved").tag("kind", "execution-failed").gauge().value()).isEqualTo(4);
        assertThat(registry.get("jobs.recovery.oldest.age").tag("kind", "execution-failed").gauge().value()).isEqualTo(60);
        assertThat(registry.get("jobs.recovery.snapshot.age").gauge().value()).isZero();
        doThrow(new IllegalStateException("database unavailable")).when(deliveries).markStaleAttemptsUnknown();
        monitor.check();
        assertThat(registry.get("jobs.recovery.monitor.failures").counter().count()).isEqualTo(1);
        assertThat(registry.get("jobs.recovery.unresolved").tag("kind", "email-unknown").gauge().value()).isEqualTo(2);
    }
}
