package com.kartoush.config.jobs;

import com.kartoush.auth.service.PasswordTokenCleanupResult;
import com.kartoush.auth.service.PasswordTokenCleanupService;
import com.kartoush.customer.service.ActivationTokenCleanupService;
import com.kartoush.platform.jobs.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

public class CleanupExpiredTokensJobHandler implements JobHandler<CleanupExpiredTokensJobRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupExpiredTokensJobHandler.class);

    private final ActivationTokenCleanupService activationTokenCleanupService;

    private final PasswordTokenCleanupService passwordTokenCleanupService;

    private final Clock clock;

    public CleanupExpiredTokensJobHandler(
        final ActivationTokenCleanupService activationTokenCleanupService,
        final PasswordTokenCleanupService passwordTokenCleanupService,
        final Clock clock
    ) {
        this.activationTokenCleanupService = activationTokenCleanupService;
        this.passwordTokenCleanupService = passwordTokenCleanupService;
        this.clock = clock;
    }

    @Override
    public void handle(final CleanupExpiredTokensJobRequest request) {
        final Instant expiresBefore = Instant.now(clock);
        final long deletedActivationTokenCount = activationTokenCleanupService.deleteExpiredTokens(expiresBefore);
        final PasswordTokenCleanupResult deletedPasswordTokens =
            passwordTokenCleanupService.deleteExpiredTokens(expiresBefore);

        LOG.info(
            "Deleted expired tokens: activation={}, passwordReset={}, passwordSetup={}",
            deletedActivationTokenCount,
            deletedPasswordTokens.passwordResetDeletedCount(),
            deletedPasswordTokens.passwordSetupDeletedCount()
        );
    }
}
