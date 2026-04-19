package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.platform.validation.RequestValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateCustomerRequestValidatorTest {

    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String VALID_EMAIL = "jack@kartoush.com";
    private static final String INVALID_EMAIL = "@";
    private static final String VALID_PHONE = "+16305551234";
    private static final String INVALID_PHONE = "abc123";

    private final CreateCustomerRequestValidator validator = new CreateCustomerRequestValidator();

    @Test
    void shouldAllowValidRequest() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE);

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowValidRequestWithNullPhoneNumber() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            null);

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowValidRequestWithBlankPhoneNumber() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            " ");

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenRequestIsNull() {
        assertThatThrownBy(() -> validator.validate(null))
            .isInstanceOf(RequestValidationException.class)
            .hasMessage("Request validation failed");
    }

    @Test
    void shouldThrowWhenFirstNameIsNull() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            null,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE);

        assertValidationError(request, "firstName");
    }

    @Test
    void shouldThrowWhenFirstNameIsBlank() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            " ",
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE);

        assertValidationError(request, "firstName");
    }

    @Test
    void shouldThrowWhenLastNameIsNull() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            null,
            VALID_EMAIL,
            VALID_PHONE);

        assertValidationError(request, "lastName");
    }

    @Test
    void shouldThrowWhenLastNameIsBlank() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            " ",
            VALID_EMAIL,
            VALID_PHONE);

        assertValidationError(request, "lastName");
    }

    @Test
    void shouldThrowWhenEmailIsNull() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            null,
            VALID_PHONE);

        assertValidationError(request, "email");
    }

    @Test
    void shouldThrowWhenEmailIsBlank() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            " ",
            VALID_PHONE);

        assertValidationError(request, "email");
    }

    @Test
    void shouldThrowWhenEmailIsInvalid() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            INVALID_EMAIL,
            VALID_PHONE);

        assertValidationError(request, "email");
    }

    @Test
    void shouldThrowWhenPhoneNumberIsInvalid() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            INVALID_PHONE);

        assertValidationError(request, "phoneNumber");
    }

    private void assertValidationError(final CreateCustomerRequest request, final String field) {
        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOfSatisfying(RequestValidationException.class, exception -> {
                assertThat(exception.getErrors())
                    .extracting(error -> error.field())
                    .contains(field);
            });
    }
}
