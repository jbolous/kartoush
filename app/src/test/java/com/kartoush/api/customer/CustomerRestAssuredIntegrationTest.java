package com.kartoush.api.customer;

import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.testsupport.HttpSpringIntegrationTest;
import com.kartoush.testsupport.PostgresRestAssuredIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

@HttpSpringIntegrationTest
class CustomerRestAssuredIntegrationTest extends PostgresRestAssuredIntegrationTest {

    private static final String BASE_URL = "/api/customers";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "+13125550100";
    private static final String EMAIL_LOCAL_PART_PREFIX = "jack+";
    private static final String EMAIL_DOMAIN = "@kartoush.com";

    @Autowired
    private UlidGenerator ulidGenerator;

    @Test
    void shouldCreateCustomerThroughHttp() {
        // given
        final String email = EMAIL_LOCAL_PART_PREFIX + ulidGenerator.next().toLowerCase() + EMAIL_DOMAIN;
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            email,
            PHONE_NUMBER);

        // when + then
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
        .when()
            .post(BASE_URL)
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .header("Location", startsWith(BASE_URL + "/"))
            .body("customerId", not(blankOrNullString()))
            .body("firstName", equalTo(FIRST_NAME))
            .body("lastName", equalTo(LAST_NAME))
            .body("email", equalTo(email))
            .body("phoneNumber", equalTo(PHONE_NUMBER))
            .body("status", equalTo(CustomerStatus.PENDING.name()));
    }
}
