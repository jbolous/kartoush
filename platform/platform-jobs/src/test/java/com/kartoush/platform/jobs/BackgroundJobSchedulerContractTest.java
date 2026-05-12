package com.kartoush.platform.jobs;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BackgroundJobSchedulerContractTest {

    @Test
    void shouldAllowEnqueueingTypedJobRequests() {
        final RecordingBackgroundJobScheduler scheduler = new RecordingBackgroundJobScheduler();
        final ExampleJobRequest request = new ExampleJobRequest("01HXJOB0000000000000000001");

        scheduler.enqueue(request);

        assertThat(scheduler.enqueuedRequest).isEqualTo(request);
        assertThat(scheduler.scheduledRequest).isNull();
        assertThat(scheduler.scheduledAt).isNull();
    }

    @Test
    void shouldAllowSchedulingTypedJobRequestsForLaterExecution() {
        final RecordingBackgroundJobScheduler scheduler = new RecordingBackgroundJobScheduler();
        final ExampleJobRequest request = new ExampleJobRequest("01HXJOB0000000000000000001");
        final Instant scheduledAt = Instant.parse("2026-05-12T12:00:00Z");

        scheduler.schedule(request, scheduledAt);

        assertThat(scheduler.scheduledRequest).isEqualTo(request);
        assertThat(scheduler.scheduledAt).isEqualTo(scheduledAt);
        assertThat(scheduler.enqueuedRequest).isNull();
    }

    private record ExampleJobRequest(String customerId) implements JobRequest {
    }

    private static final class RecordingBackgroundJobScheduler implements BackgroundJobScheduler {

        private JobRequest enqueuedRequest;
        private JobRequest scheduledRequest;
        private Instant scheduledAt;

        @Override
        public void enqueue(final JobRequest request) {
            this.enqueuedRequest = request;
        }

        @Override
        public void schedule(final JobRequest request, final Instant scheduledAt) {
            this.scheduledRequest = request;
            this.scheduledAt = scheduledAt;
        }
    }
}
