package com.kartoush.auth.service.impl;

import com.kartoush.auth.persistence.repository.PasswordResetTokenRepository;
import com.kartoush.auth.persistence.repository.PasswordSetupTokenRepository;
import com.kartoush.auth.service.PasswordTokenCleanupResult;
import com.kartoush.auth.service.PasswordTokenCleanupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DefaultPasswordTokenCleanupService implements PasswordTokenCleanupService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final PasswordSetupTokenRepository passwordSetupTokenRepository;

    public DefaultPasswordTokenCleanupService(
        final PasswordResetTokenRepository passwordResetTokenRepository,
        final PasswordSetupTokenRepository passwordSetupTokenRepository
    ) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordSetupTokenRepository = passwordSetupTokenRepository;
    }

    @Override
    @Transactional
    public PasswordTokenCleanupResult deleteExpiredTokens(final Instant expiresBefore) {
        final long passwordResetDeletedCount = passwordResetTokenRepository.deleteByExpiresAtBefore(expiresBefore);
        final long passwordSetupDeletedCount = passwordSetupTokenRepository.deleteByExpiresAtBefore(expiresBefore);

        return new PasswordTokenCleanupResult(passwordResetDeletedCount, passwordSetupDeletedCount);
    }
}
