package com.kartoush.api.customer;

import com.kartoush.api.error.ErrorCode;
import com.kartoush.customer.persistence.entity.TermsAcceptanceEntity;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.customer.persistence.repository.TermsAcceptanceRepository;
import com.kartoush.platform.types.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.List;

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
    private static final String CURRENT_TERMS_VERSION = "2026.04.01";

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TermsAcceptanceRepository termsAcceptanceRepository;

    @Test
    void shouldCreateCustomerThroughHttp() {
        // given
        final String email = uniqueEmail();
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            email,
            PHONE_NUMBER,
            true,
            CURRENT_TERMS_VERSION);

        // when + then
        final String customerId = given()
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
            .body("status", equalTo(CustomerStatus.PENDING.name()))
            .extract()
            .path("customerId");

        final List<TermsAcceptanceEntity> acceptances =
            termsAcceptanceRepository.findAllByCustomerIdOrderByAcceptedAtAsc(CustomerIdEmbeddable.from(customerId));

        assertThat(acceptances).hasSize(1);
        assertThat(acceptances.getFirst().getTermsVersion()).isEqualTo(CURRENT_TERMS_VERSION);
        assertThat(acceptances.getFirst().getAcceptedAt()).isNotNull();
    }

    @Test
    void shouldReturnValidationProblemForInvalidEmail() {
        // given
        final long customerCountBeforeRequest = customerRepository.count();
        final long termsAcceptanceCountBeforeRequest = termsAcceptanceRepository.count();
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            INVALID_EMAIL,
            PHONE_NUMBER,
            true,
            CURRENT_TERMS_VERSION);

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
        assertThat(termsAcceptanceRepository.count()).isEqualTo(termsAcceptanceCountBeforeRequest);
    }

    @Test
    void shouldReturnValidationProblemForInvalidPhoneNumber() {
        // given
        final long customerCountBeforeRequest = customerRepository.count();
        final long termsAcceptanceCountBeforeRequest = termsAcceptanceRepository.count();
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            uniqueEmail(),
            "abc123",
            true,
            CURRENT_TERMS_VERSION);

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
        assertThat(termsAcceptanceRepository.count()).isEqualTo(termsAcceptanceCountBeforeRequest);
    }

    @Test
    void shouldReturnValidationProblemWhenTermsAreNotAccepted() {
        // given
        final long customerCountBeforeRequest = customerRepository.count();
        final long termsAcceptanceCountBeforeRequest = termsAcceptanceRepository.count();
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            uniqueEmail(),
            PHONE_NUMBER,
            false,
            CURRENT_TERMS_VERSION);

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
            .body("errors[0].field", equalTo("termsAccepted"));

        assertThat(customerRepository.count()).isEqualTo(customerCountBeforeRequest);
        assertThat(termsAcceptanceRepository.count()).isEqualTo(termsAcceptanceCountBeforeRequest);
    }

    @Test
    void shouldReturnValidationProblemWhenTermsVersionIsInvalid() {
        // given
        final long customerCountBeforeRequest = customerRepository.count();
        final long termsAcceptanceCountBeforeRequest = termsAcceptanceRepository.count();
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            uniqueEmail(),
            PHONE_NUMBER,
            true,
            "2026-03");

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
            .body("errors[0].field", equalTo("termsVersion"));

        assertThat(customerRepository.count()).isEqualTo(customerCountBeforeRequest);
        assertThat(termsAcceptanceRepository.count()).isEqualTo(termsAcceptanceCountBeforeRequest);
    }
}
