package com.kartoush.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.auth.service.PasswordTokenCleanupService;
import com.kartoush.config.jobs.CleanupExpiredTokensJobRequest;
import com.kartoush.config.jobs.CleanupJobConfiguration;
import com.kartoush.customer.service.ActivationTokenCleanupService;
import com.kartoush.platform.jobs.RecurringBackgroundJobScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CleanupJobConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withBean(RecurringBackgroundJobScheduler.class, () -> mock(RecurringBackgroundJobScheduler.class))
        .withBean(ActivationTokenCleanupService.class, () -> mock(ActivationTokenCleanupService.class))
        .withBean(PasswordTokenCleanupService.class, () -> mock(PasswordTokenCleanupService.class))
        .withBean(Clock.class, Clock::systemUTC)
        .withUserConfiguration(CleanupJobConfiguration.class);

    @Test
    void shouldRegisterRecurringCleanupJobWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RecurringBackgroundJobScheduler.class);

            final RecurringBackgroundJobScheduler recurringBackgroundJobScheduler =
                context.getBean(RecurringBackgroundJobScheduler.class);

            context.getBean(ApplicationRunner.class).run(null);

            verify(recurringBackgroundJobScheduler).scheduleRecurrently(
                "cleanup-expired-tokens",
                "0 0 3 * * *",
                new CleanupExpiredTokensJobRequest()
            );
        });
    }

    @Test
    void shouldNotRegisterRecurringCleanupJobWhenDisabled() {
        contextRunner
            .withPropertyValues("kartoush.jobs.cleanup.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();

                final RecurringBackgroundJobScheduler recurringBackgroundJobScheduler =
                    context.getBean(RecurringBackgroundJobScheduler.class);

                context.getBean(ApplicationRunner.class).run(null);

                verifyNoInteractions(recurringBackgroundJobScheduler);
            });
    }
}
