package com.kartoush.platform.jobs;

/**
 * Registers durable recurring background work without exposing infrastructure
 * details to application code.
 */
public interface RecurringBackgroundJobScheduler {

    void scheduleRecurrently(String recurringJobId, String cronExpression, JobRequest request);
}
