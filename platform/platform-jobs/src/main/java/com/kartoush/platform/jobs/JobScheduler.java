package com.kartoush.platform.jobs;

import java.time.Instant;

/**
 * Schedules durable background work without exposing infrastructure details to
 * application code.
 */
public interface JobScheduler {

    void enqueue(JobRequest request);

    void schedule(JobRequest request, Instant scheduledAt);
}
