package com.kartoush.config.jobs;

import com.kartoush.auth.service.PasswordTokenCleanupResult;
import com.kartoush.auth.service.PasswordTokenCleanupService;
import com.kartoush.customer.service.ActivationTokenCleanupService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CleanupExpiredTokensJobHandlerTest {

    private static final Instant NOW = Instant.parse("2026-05-18T03:00:00Z");

    private final ActivationTokenCleanupService activationTokenCleanupService = mock(ActivationTokenCleanupService.class);

    private final PasswordTokenCleanupService passwordTokenCleanupService = mock(PasswordTokenCleanupService.class);

    private final CleanupExpiredTokensJobHandler handler =
        new CleanupExpiredTokensJobHandler(
            activationTokenCleanupService,
            passwordTokenCleanupService,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

    @Test
    void shouldDeleteExpiredTokensAcrossModules() {
        when(activationTokenCleanupService.deleteExpiredTokens(NOW)).thenReturn(4L);
        when(passwordTokenCleanupService.deleteExpiredTokens(NOW)).thenReturn(new PasswordTokenCleanupResult(3L, 2L));

        handler.handle(new CleanupExpiredTokensJobRequest());

        verify(activationTokenCleanupService).deleteExpiredTokens(NOW);
        verify(passwordTokenCleanupService).deleteExpiredTokens(NOW);
    }
}
