package com.kartoush.customer.service.impl;

import com.kartoush.customer.persistence.repository.ActivationTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultActivationTokenCleanupServiceTest {

    private final ActivationTokenRepository activationTokenRepository = mock(ActivationTokenRepository.class);

    private final DefaultActivationTokenCleanupService service =
        new DefaultActivationTokenCleanupService(activationTokenRepository);

    @Test
    void shouldDeleteExpiredActivationTokens() {
        final Instant expiresBefore = Instant.parse("2026-05-18T03:00:00Z");

        when(activationTokenRepository.deleteByExpiresAtBefore(expiresBefore)).thenReturn(4L);

        final long deletedCount = service.deleteExpiredTokens(expiresBefore);

        assertThat(deletedCount).isEqualTo(4L);
        verify(activationTokenRepository).deleteByExpiresAtBefore(expiresBefore);
    }
}
