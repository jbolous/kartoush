package com.kartoush.config.jobs;

import com.kartoush.auth.service.PasswordTokenCleanupService;
import com.kartoush.customer.service.ActivationTokenCleanupService;
import com.kartoush.platform.jobs.JobHandler;
import com.kartoush.platform.jobs.RecurringBackgroundJobScheduler;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(CleanupJobProperties.class)
public class CleanupJobConfiguration {

    static final String EXPIRED_TOKEN_CLEANUP_JOB_ID = "cleanup-expired-tokens";

    @Bean
    JobHandler<CleanupExpiredTokensJobRequest> cleanupExpiredTokensJobHandler(
        final ActivationTokenCleanupService activationTokenCleanupService,
        final PasswordTokenCleanupService passwordTokenCleanupService,
        final Clock clock
    ) {
        return new CleanupExpiredTokensJobHandler(
            activationTokenCleanupService,
            passwordTokenCleanupService,
            clock
        );
    }

    @Bean
    ApplicationRunner cleanupJobRegistrar(
        final RecurringBackgroundJobScheduler recurringBackgroundJobScheduler,
        final CleanupJobProperties cleanupJobProperties
    ) {
        return args -> {
            if (!cleanupJobProperties.isEnabled()) {
                return;
            }

            recurringBackgroundJobScheduler.scheduleRecurrently(
                EXPIRED_TOKEN_CLEANUP_JOB_ID,
                cleanupJobProperties.getExpiredTokenCron(),
                new CleanupExpiredTokensJobRequest()
            );
        };
    }
}
