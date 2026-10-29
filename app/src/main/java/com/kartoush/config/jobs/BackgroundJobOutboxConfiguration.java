package com.kartoush.config.jobs;

import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import java.time.Clock;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(BackgroundJobOutboxProperties.class)
@ConditionalOnProperty(name = "jobrunr.background-job-server.enabled", havingValue = "true")
public class BackgroundJobOutboxConfiguration {
    @Bean
    BackgroundJobOutboxRelay backgroundJobOutboxRelay(final BackgroundJobOutboxRepository repository,
        final JobScheduler scheduler, final OutboxJobExecutor executor,
        final PlatformTransactionManager transactionManager, final BackgroundJobOutboxProperties properties,
        final Clock clock) {
        return new BackgroundJobOutboxRelay(repository, scheduler, executor, transactionManager, properties, clock);
    }
}
