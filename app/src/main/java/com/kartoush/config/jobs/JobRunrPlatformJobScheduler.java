package com.kartoush.config.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.jobs.JobRequest;
import com.kartoush.platform.jobs.JobSchedulingException;
import org.jobrunr.scheduling.JobScheduler;

import java.time.Instant;

public class JobRunrPlatformJobScheduler implements BackgroundJobScheduler {

    private final JobScheduler jobRunrJobScheduler;

    private final PlatformJobDispatcher platformJobDispatcher;

    private final ObjectMapper objectMapper;

    public JobRunrPlatformJobScheduler(
        final JobScheduler jobRunrJobScheduler,
        final PlatformJobDispatcher platformJobDispatcher,
        final ObjectMapper objectMapper) {
        this.jobRunrJobScheduler = jobRunrJobScheduler;
        this.platformJobDispatcher = platformJobDispatcher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enqueue(final JobRequest request) {
        enqueueSerialized(snapshot(request));
    }

    void enqueueSerialized(final SerializedJobRequest serializedJobRequest) {
        try {
            jobRunrJobScheduler.enqueue(() -> platformJobDispatcher.dispatch(
                serializedJobRequest.requestType(),
                serializedJobRequest.payload()
            ));
        } catch (final RuntimeException ex) {
            throw new JobSchedulingException("Failed to enqueue job request of type " + serializedJobRequest.requestType(), ex);
        }
    }

    @Override
    public void schedule(final JobRequest request, final Instant scheduledAt) {
        scheduleSerialized(snapshot(request), scheduledAt);
    }

    void scheduleSerialized(final SerializedJobRequest serializedJobRequest, final Instant scheduledAt) {
        try {
            jobRunrJobScheduler.schedule(scheduledAt, () -> platformJobDispatcher.dispatch(
                serializedJobRequest.requestType(),
                serializedJobRequest.payload()
            ));
        } catch (final RuntimeException ex) {
            throw new JobSchedulingException("Failed to schedule job request of type " + serializedJobRequest.requestType(), ex);
        }
    }

    SerializedJobRequest snapshot(final JobRequest request) {
        try {
            return new SerializedJobRequest(
                request.getClass().getName(),
                objectMapper.writeValueAsString(request)
            );
        } catch (final JsonProcessingException ex) {
            throw new JobSchedulingException("Failed to serialize job request of type " + request.getClass().getName(), ex);
        }
    }

    record SerializedJobRequest(String requestType, String payload) {
    }
}
