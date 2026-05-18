package com.kartoush.config.jobs;

import com.kartoush.platform.jobs.JobRequest;
import com.kartoush.platform.jobs.JobSchedulingException;
import com.kartoush.platform.jobs.RecurringBackgroundJobScheduler;
import org.jobrunr.scheduling.JobScheduler;

public class JobRunrRecurringBackgroundJobScheduler implements RecurringBackgroundJobScheduler {

    private final JobScheduler jobRunrJobScheduler;

    private final JobRunrPlatformJobScheduler jobRunrPlatformJobScheduler;

    private final PlatformJobDispatcher platformJobDispatcher;

    public JobRunrRecurringBackgroundJobScheduler(
        final JobScheduler jobRunrJobScheduler,
        final JobRunrPlatformJobScheduler jobRunrPlatformJobScheduler,
        final PlatformJobDispatcher platformJobDispatcher
    ) {
        this.jobRunrJobScheduler = jobRunrJobScheduler;
        this.jobRunrPlatformJobScheduler = jobRunrPlatformJobScheduler;
        this.platformJobDispatcher = platformJobDispatcher;
    }

    @Override
    public void scheduleRecurrently(
        final String recurringJobId,
        final String cronExpression,
        final JobRequest request
    ) {
        final JobRunrPlatformJobScheduler.SerializedJobRequest serializedJobRequest =
            jobRunrPlatformJobScheduler.snapshot(request);

        try {
            jobRunrJobScheduler.scheduleRecurrently(
                recurringJobId,
                cronExpression,
                () -> platformJobDispatcher.dispatch(
                    serializedJobRequest.requestType(),
                    serializedJobRequest.payload()
                )
            );
        } catch (final RuntimeException ex) {
            throw new JobSchedulingException(
                "Failed to register recurring job request of type " + serializedJobRequest.requestType(),
                ex
            );
        }
    }
}
