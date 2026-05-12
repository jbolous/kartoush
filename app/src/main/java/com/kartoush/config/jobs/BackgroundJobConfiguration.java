package com.kartoush.config.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.jobs.JobHandler;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BackgroundJobConfiguration {

    @Bean
    PlatformJobDispatcher platformJobDispatcher(
        final ObjectMapper objectMapper,
        final List<JobHandler<?>> jobHandlers) {
        return new PlatformJobDispatcher(objectMapper, jobHandlers);
    }

    @Bean
    BackgroundJobScheduler platformJobScheduler(
        final JobScheduler jobRunrJobScheduler,
        final PlatformJobDispatcher platformJobDispatcher,
        final ObjectMapper objectMapper) {
        return new TransactionAwareBackgroundJobScheduler(
            new JobRunrPlatformJobScheduler(jobRunrJobScheduler, platformJobDispatcher, objectMapper)
        );
    }
}
