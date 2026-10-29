package com.kartoush.config.jobs;

import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.jobs.JobHandler;
import com.kartoush.platform.jobs.RecurringBackgroundJobScheduler;
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
        final JobScheduler jobScheduler,
        final PlatformJobDispatcher platformJobDispatcher,
        final ObjectMapper objectMapper,
        final BackgroundJobOutboxRepository outboxRepository) {
        return new TransactionAwareBackgroundJobScheduler(
            jobSchedulerAdapter(jobScheduler, platformJobDispatcher, objectMapper),
            outboxRepository
        );
    }

    @Bean
    RecurringBackgroundJobScheduler recurringBackgroundJobScheduler(
        final JobScheduler jobScheduler,
        final PlatformJobDispatcher platformJobDispatcher,
        final ObjectMapper objectMapper) {
        return new JobRunrRecurringBackgroundJobScheduler(
            jobScheduler,
            jobSchedulerAdapter(jobScheduler, platformJobDispatcher, objectMapper),
            platformJobDispatcher
        );
    }

    private JobRunrPlatformJobScheduler jobSchedulerAdapter(
        final JobScheduler jobScheduler,
        final PlatformJobDispatcher platformJobDispatcher,
        final ObjectMapper objectMapper
    ) {
        return new JobRunrPlatformJobScheduler(jobScheduler, platformJobDispatcher, objectMapper);
    }
}
