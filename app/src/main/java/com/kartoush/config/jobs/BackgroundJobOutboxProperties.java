package com.kartoush.config.jobs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import java.time.Duration;

@ConfigurationProperties("kartoush.jobs.outbox")
public record BackgroundJobOutboxProperties(
    @DefaultValue("5") int maxPublicationFailures,
    @DefaultValue("30s") Duration initialBackoff,
    @DefaultValue("1h") Duration maxBackoff
) {
    public BackgroundJobOutboxProperties {
        if (maxPublicationFailures < 1 || initialBackoff == null || initialBackoff.isNegative()
            || initialBackoff.isZero() || maxBackoff == null || maxBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalArgumentException("Outbox failure limit must be positive and backoff bounds valid");
        }
    }

    public Duration backoffAfter(final int previousFailures) {
        Duration delay = initialBackoff;
        for (int index = 0; index < previousFailures && delay.compareTo(maxBackoff) < 0; index++) {
            // Avoid multiplication overflow even for large configured durations.
            delay = delay.compareTo(maxBackoff.dividedBy(2)) > 0 ? maxBackoff : delay.multipliedBy(2);
        }
        return delay.compareTo(maxBackoff) > 0 ? maxBackoff : delay;
    }
}
