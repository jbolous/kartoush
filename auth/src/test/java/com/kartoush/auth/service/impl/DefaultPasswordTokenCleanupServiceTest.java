package com.kartoush.auth.service.impl;

import com.kartoush.auth.persistence.repository.PasswordResetTokenRepository;
import com.kartoush.auth.persistence.repository.PasswordSetupTokenRepository;
import com.kartoush.auth.service.PasswordTokenCleanupResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultPasswordTokenCleanupServiceTest {

    private final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);

    private final PasswordSetupTokenRepository passwordSetupTokenRepository = mock(PasswordSetupTokenRepository.class);

    private final DefaultPasswordTokenCleanupService service =
        new DefaultPasswordTokenCleanupService(passwordResetTokenRepository, passwordSetupTokenRepository);

    @Test
    void shouldDeleteExpiredPasswordTokens() {
        final Instant expiresBefore = Instant.parse("2026-05-18T03:00:00Z");

        when(passwordResetTokenRepository.deleteByExpiresAtBefore(expiresBefore)).thenReturn(3L);
        when(passwordSetupTokenRepository.deleteByExpiresAtBefore(expiresBefore)).thenReturn(2L);

        final PasswordTokenCleanupResult result = service.deleteExpiredTokens(expiresBefore);

        assertThat(result.passwordResetDeletedCount()).isEqualTo(3L);
        assertThat(result.passwordSetupDeletedCount()).isEqualTo(2L);
        verify(passwordResetTokenRepository).deleteByExpiresAtBefore(expiresBefore);
        verify(passwordSetupTokenRepository).deleteByExpiresAtBefore(expiresBefore);
    }
}
