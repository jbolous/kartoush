package com.kartoush.testsupport;

import java.time.Duration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class PostgresDataJpaTest {

    private static final String POSTGRES_DOCKER_IMAGE_NAME = "postgres:16";
    private static final String DATABASE_NAME = "kartoush";
    private static final String USERNAME = "kartoush";
    private static final String PASSWORD = "kartoush";

    private static final String APP_LABEL_KEY = "app";
    private static final String ENV_LABEL_KEY = "env";
    private static final String APP_LABEL_VALUE = "kartoush";
    private static final String ENV_LABEL_VALUE = "test";

    private static final int STARTUP_ATTEMPTS = 3;
    private static final Duration STARTUP_TIMEOUT_DURATION = Duration.ofSeconds(60);

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer(POSTGRES_DOCKER_IMAGE_NAME)
            .withDatabaseName(DATABASE_NAME)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withLabel(APP_LABEL_KEY, APP_LABEL_VALUE)
            .withLabel(ENV_LABEL_KEY, ENV_LABEL_VALUE)
            .withStartupAttempts(STARTUP_ATTEMPTS)
            .withStartupTimeout(STARTUP_TIMEOUT_DURATION);

    static {
        POSTGRES.start();
    }
}
