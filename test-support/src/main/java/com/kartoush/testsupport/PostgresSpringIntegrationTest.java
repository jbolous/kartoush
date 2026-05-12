package com.kartoush.testsupport;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;

public abstract class PostgresSpringIntegrationTest {

    private static final String POSTGRES_DOCKER_IMAGE_NAME = "postgres:16-alpine";

    private static final String DATABASE_NAME = "kartoush";

    private static final String USERNAME = "kartoush";

    private static final String PASSWORD = "kartoush";

    private static final int STARTUP_ATTEMPTS = 3;

    private static final Duration STARTUP_TIMEOUT_DURATION = Duration.ofSeconds(60);

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer(POSTGRES_DOCKER_IMAGE_NAME)
            .withDatabaseName(DATABASE_NAME)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withStartupAttempts(STARTUP_ATTEMPTS)
            .withStartupTimeout(STARTUP_TIMEOUT_DURATION);

    static {
        POSTGRES.start();
    }
}
