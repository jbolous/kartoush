package com.kartoush.testsupport;

import java.time.Duration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
public abstract class PostgresSpringIntegrationTest
{
    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("kartoush")
            .withUsername("kartoush")
            .withPassword("kartoush")
            .withStartupAttempts(3)
            .withStartupTimeout(Duration.ofSeconds(60));
}
