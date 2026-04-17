package com.kartoush.testsupport;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

public abstract class PostgresRestAssuredIntegrationTest extends PostgresSpringIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterEach
    void resetRestAssured() {
        RestAssured.reset();
    }
}
