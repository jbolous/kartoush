package com.kartoush.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.config.jobs.PlatformJobDispatcher;
import com.kartoush.platform.jobs.JobHandler;
import com.kartoush.platform.jobs.JobRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformJobDispatcherTest {

    @Test
    void shouldDeserializeRequestAndDispatchToMatchingHandler() throws Exception {
        final RecordingJobHandler jobHandler = new RecordingJobHandler();
        final PlatformJobDispatcher dispatcher =
            new PlatformJobDispatcher(new ObjectMapper(), java.util.List.of(jobHandler));
        final ExampleJobRequest request = new ExampleJobRequest("01JOBJOBRUNR00000000000001");

        dispatcher.dispatch(
            ExampleJobRequest.class.getName(),
            new ObjectMapper().writeValueAsString(request)
        );

        assertThat(jobHandler.handledRequest).isEqualTo(request);
    }

    record ExampleJobRequest(String customerId) implements JobRequest {
    }

    static final class RecordingJobHandler implements JobHandler<ExampleJobRequest> {

        private ExampleJobRequest handledRequest;

        @Override
        public void handle(final ExampleJobRequest request) {
            this.handledRequest = request;
        }
    }
}
