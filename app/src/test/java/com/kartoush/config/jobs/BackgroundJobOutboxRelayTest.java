package com.kartoush.config.jobs;

import org.junit.jupiter.api.Test;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackgroundJobOutboxRelayTest {
    @Test
    void shouldCapExponentialBackoffWhenFailuresAccumulate() {
        final var properties = new BackgroundJobOutboxProperties(5, Duration.ofSeconds(30), Duration.ofMinutes(2));
        assertThat(properties.backoffAfter(0)).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.backoffAfter(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(properties.backoffAfter(2)).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.backoffAfter(Integer.MAX_VALUE)).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void shouldRejectInvalidPolicyWhenConfigurationIsCreated() {
        assertThatThrownBy(() -> new BackgroundJobOutboxProperties(0, Duration.ofSeconds(1), Duration.ofSeconds(2)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BackgroundJobOutboxProperties(5, Duration.ZERO, Duration.ofSeconds(2)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BackgroundJobOutboxProperties(5, Duration.ofSeconds(3), Duration.ofSeconds(2)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
