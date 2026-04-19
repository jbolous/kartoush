package com.kartoush.api.customer;

import com.kartoush.api.error.ErrorCode;
import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.platform.types.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

class CustomerCreationRestAssuredIntegrationTest extends AbstractCustomerRestAssuredIntegrationTest {

    private static final String INVALID_EMAIL = "not-an-email";

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldCreateCustomerThroughHttp() {
        // given
        final String email = uniqueEmail();
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

    @Test
    void shouldReturnValidationProblemForInvalidEmail() {
        // given
        final long customerCountBeforeRequest = customerRepository.count();
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            INVALID_EMAIL,
            PHONE_NUMBER);

        // when + then
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
        .when()
            .post(BASE_URL)
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("title", equalTo("Request Validation Failed"))
            .body("detail", equalTo("Request validation failed"))
            .body("errorCode", equalTo(ErrorCode.VALIDATION_FAILED.name()))
            .body("type", equalTo(ErrorCode.VALIDATION_FAILED.urn()))
            .body("instance", equalTo(BASE_URL))
            .body("timestamp", notNullValue())
            .body("errors", hasSize(1))
            .body("errors[0].field", equalTo("email"))
            .body("errors[0].message", not(blankOrNullString()))
            .body("errors[0].rejectedValue", nullValue());

        assertThat(customerRepository.count()).isEqualTo(customerCountBeforeRequest);
    }

    @Test
    void shouldReturnValidationProblemForInvalidPhoneNumber() {
        // given
        final long customerCountBeforeRequest = customerRepository.count();
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            uniqueEmail(),
            "abc123");

        // when + then
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
        .when()
            .post(BASE_URL)
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("title", equalTo("Request Validation Failed"))
            .body("detail", equalTo("Request validation failed"))
            .body("errorCode", equalTo(ErrorCode.VALIDATION_FAILED.name()))
            .body("type", equalTo(ErrorCode.VALIDATION_FAILED.urn()))
            .body("instance", equalTo(BASE_URL))
            .body("timestamp", notNullValue())
            .body("errors", hasSize(1))
            .body("errors[0].field", equalTo("phoneNumber"))
            .body("errors[0].message", not(blankOrNullString()))
            .body("errors[0].rejectedValue", nullValue());

        assertThat(customerRepository.count()).isEqualTo(customerCountBeforeRequest);
    }
}
