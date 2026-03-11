package com.kartoush.customer.internal.validation;


import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.platform.validation.RequestValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateCustomerRequestValidatorTest {

    private static final String EMAIL = "jack@kartoush.com";
    private static final String INVALID_EMAIL = "@";
    private static final String PASSWORD = "password";
    private static final String PHONE_NUMBER = "3125882300";
    private static final String INVALID_PHONE_NUMBER = "abc-123";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";

    private final CreateCustomerRequestValidator validator = new CreateCustomerRequestValidator();

    @Test
    @DisplayName("Should reject null request")
    void shouldRejectNullRequest() {
        // then
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(RequestValidationException.class);
    }

    @Test
    @DisplayName("Should allow a blank phone number")
    void shouldAllowBlankPhoneNumber() {
        // given
        final var request = new CreateCustomerRequest(
                EMAIL,
                PASSWORD,
                " ",
                FIRST_NAME,
                LAST_NAME);

        // then
        assertThatCode(() ->
                validator.validate(request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject an invalid phone number")
    void shouldRejectInvalidPhoneNumberWhenPresent() {

        // given
        final var request = new CreateCustomerRequest(
                EMAIL,
                PASSWORD,
                INVALID_PHONE_NUMBER,
                FIRST_NAME,
                LAST_NAME);

        // then
        assertThatThrownBy(() ->
                validator.validate(request))
                .isInstanceOf(RequestValidationException.class);
    }

    @Test
    @DisplayName("Should reject invalid email")
    void shouldRejectInvalidEmailWhenPresent() {

        // given
        final var request = new CreateCustomerRequest(
                INVALID_EMAIL,
                PASSWORD,
                PHONE_NUMBER,
                FIRST_NAME,
                LAST_NAME);

        // then
        assertThatThrownBy(() ->
                validator.validate(request))
                .isInstanceOf(RequestValidationException.class);
    }

    @Test
    @DisplayName("Should reject missing first and last name")
    void shouldRejectMissingFirstAndLastName() {
        // given
        final var request = new CreateCustomerRequest(
                EMAIL,
                PASSWORD,
                PHONE_NUMBER,
                "",
                "");

        // then
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(RequestValidationException.class);
    }
}
