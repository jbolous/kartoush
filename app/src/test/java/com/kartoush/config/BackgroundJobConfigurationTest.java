package com.kartoush.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.config.jobs.BackgroundJobConfiguration;
import com.kartoush.config.jobs.PlatformJobDispatcher;
import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.jobs.JobHandler;
import com.kartoush.platform.jobs.JobRequest;
import org.junit.jupiter.api.Test;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BackgroundJobConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withBean(JobScheduler.class, () -> org.mockito.Mockito.mock(JobScheduler.class))
        .withBean(JobHandler.class, ExampleJobHandler::new)
        .withUserConfiguration(BackgroundJobConfiguration.class);

    @Test
    void shouldProvidePlatformJobSchedulerBackedByJobRunr() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(BackgroundJobScheduler.class);
            assertThat(context).hasSingleBean(PlatformJobDispatcher.class);
        });
    }

    record ExampleJobRequest(String customerId) implements JobRequest {
    }

    static final class ExampleJobHandler implements JobHandler<ExampleJobRequest> {

        @Override
        public void handle(final ExampleJobRequest request) {
        }
    }
}
