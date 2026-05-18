package com.kartoush.customer.service.impl;

import com.kartoush.customer.persistence.repository.ActivationTokenRepository;
import com.kartoush.customer.service.ActivationTokenCleanupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DefaultActivationTokenCleanupService implements ActivationTokenCleanupService {

    private final ActivationTokenRepository activationTokenRepository;

    public DefaultActivationTokenCleanupService(final ActivationTokenRepository activationTokenRepository) {
        this.activationTokenRepository = activationTokenRepository;
    }

    @Override
    @Transactional
    public long deleteExpiredTokens(final Instant expiresBefore) {
        return activationTokenRepository.deleteByExpiresAtBefore(expiresBefore);
    }
}
