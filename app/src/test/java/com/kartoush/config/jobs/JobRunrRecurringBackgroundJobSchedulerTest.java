package com.kartoush.config.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.platform.jobs.JobSchedulingException;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JobRunrRecurringBackgroundJobSchedulerTest {

    private static final String RECURRING_JOB_ID = "cleanup-expired-tokens";
    private static final String CRON_EXPRESSION = "0 0 3 * * *";

    private final JobScheduler jobRunrJobScheduler = mock(JobScheduler.class);

    private final PlatformJobDispatcher platformJobDispatcher = mock(PlatformJobDispatcher.class);

    private final JobRunrPlatformJobScheduler jobRunrPlatformJobScheduler =
        new JobRunrPlatformJobScheduler(jobRunrJobScheduler, platformJobDispatcher, new ObjectMapper());

    private final JobRunrRecurringBackgroundJobScheduler recurringBackgroundJobScheduler =
        new JobRunrRecurringBackgroundJobScheduler(
            jobRunrJobScheduler,
            jobRunrPlatformJobScheduler,
            platformJobDispatcher
        );

    @Test
    void shouldRegisterRecurringJob() {
        recurringBackgroundJobScheduler.scheduleRecurrently(
            RECURRING_JOB_ID,
            CRON_EXPRESSION,
            new CleanupExpiredTokensJobRequest()
        );

        verify(jobRunrJobScheduler).scheduleRecurrently(eq(RECURRING_JOB_ID), eq(CRON_EXPRESSION), any(JobLambda.class));
    }

    @Test
    void shouldWrapRecurringRegistrationFailure() {
        doThrow(new RuntimeException("boom")).when(jobRunrJobScheduler)
            .scheduleRecurrently(eq(RECURRING_JOB_ID), eq(CRON_EXPRESSION), any(JobLambda.class));

        assertThatThrownBy(() -> recurringBackgroundJobScheduler.scheduleRecurrently(
            RECURRING_JOB_ID,
            CRON_EXPRESSION,
            new CleanupExpiredTokensJobRequest()
        ))
            .isInstanceOf(JobSchedulingException.class)
            .hasMessageContaining("Failed to register recurring job request of type")
            .hasCauseInstanceOf(RuntimeException.class);
    }
}
