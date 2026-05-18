package com.kartoush.config.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.platform.jobs.BackgroundJobScheduler;
import com.kartoush.platform.jobs.JobHandler;
import com.kartoush.platform.jobs.RecurringBackgroundJobScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
        final PlatformTransactionManager transactionManager) {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        return new TransactionAwareBackgroundJobScheduler(
            jobSchedulerAdapter(jobScheduler, platformJobDispatcher, objectMapper),
            transactionTemplate
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
