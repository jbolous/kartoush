package com.kartoush.api.customer;

import com.kartoush.api.error.ErrorCode;
import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.persistence.repository.TermsAcceptanceRepository;
import com.kartoush.platform.types.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class CustomerDuplicateEmailRestAssuredIntegrationTest extends AbstractCustomerRestAssuredIntegrationTest {

    private static final String CURRENT_TERMS_VERSION = "2026.04.01";

    @Autowired
    private TermsAcceptanceRepository termsAcceptanceRepository;

    @Test
    void shouldReturnConflictProblemForDuplicateEmailWhenCustomerIsActive() {
        // given
        final String email = uniqueEmail();
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            email,
            PHONE_NUMBER,
            true,
            CURRENT_TERMS_VERSION);

        final String customerId = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
        .when()
            .post(BASE_URL)
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .path("customerId");

        final String activationToken = latestCapturedActivationEmail().rawToken();

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(new ActivateCustomerRequest(activationToken))
        .when()
            .post(BASE_URL + ACTIVATION_PATH, customerId)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("status", equalTo(CustomerStatus.ACTIVE.name()));

        final long acceptanceCountBeforeDuplicateRequest = termsAcceptanceRepository.count();

        // when + then
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
        .when()
            .post(BASE_URL)
        .then()
            .statusCode(HttpStatus.CONFLICT.value())
            .body("title", equalTo("Customer Already Exists"))
            .body("detail", equalTo("Customer already exists with email: " + email))
            .body("errorCode", equalTo(ErrorCode.CUSTOMER_ALREADY_EXISTS.name()))
            .body("type", equalTo(ErrorCode.CUSTOMER_ALREADY_EXISTS.urn()))
            .body("instance", equalTo(BASE_URL))
            .body("timestamp", notNullValue());

        assertThat(termsAcceptanceRepository.count()).isEqualTo(acceptanceCountBeforeDuplicateRequest);
    }
}
