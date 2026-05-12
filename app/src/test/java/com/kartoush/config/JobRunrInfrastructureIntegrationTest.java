package com.kartoush.config;

import com.kartoush.KartoushApplication;
import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.jobs.JobHandler;
import com.kartoush.platform.jobs.JobRequest;
import com.kartoush.testsupport.PostgresSpringIntegrationTest;
import com.kartoush.testsupport.SpringIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringIntegrationTest
@Import({KartoushApplication.class, JobRunrInfrastructureIntegrationTest.JobRunrTestConfiguration.class})
class JobRunrInfrastructureIntegrationTest extends PostgresSpringIntegrationTest {

    @Autowired
    private BackgroundJobScheduler platformJobScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldProvidePlatformJobSchedulerBean() {
        assertThat(platformJobScheduler).isNotNull();
    }

    @Test
    void shouldPersistEnqueuedJobsToPostgres() {
        platformJobScheduler.enqueue(new ExampleJobRequest("01JOBJOBRUNR00000000000001"));

        final Integer jobCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(id) FROM jobrunr.jobrunr_jobs",
            Integer.class
        );

        assertThat(jobCount).isNotNull();
        assertThat(jobCount).isGreaterThanOrEqualTo(1);
    }

    record ExampleJobRequest(String customerId) implements JobRequest {
    }

    @TestConfiguration
    static class JobRunrTestConfiguration {

        @Bean
        JobHandler<ExampleJobRequest> exampleJobHandler() {
            return new ExampleJobHandler();
        }

        static final class ExampleJobHandler implements JobHandler<ExampleJobRequest> {

            @Override
            public void handle(final ExampleJobRequest request) {
            }
        }
    }
}
