package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.UpdateCustomerRequest;
import com.kartoush.platform.validation.RequestValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateCustomerRequestValidatorTest
{
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String VALID_PHONE = "+16305551234";
    private static final String INVALID_PHONE = "abc123";
    private static final String TOO_SHORT_PHONE = "123456";
    private static final String TOO_LONG_PHONE = "+1234567890123456";

    private final UpdateCustomerRequestValidator validator = new UpdateCustomerRequestValidator();

    @Test
    void shouldAllowValidRequest() {
        final UpdateCustomerRequest request = new UpdateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_PHONE);

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowValidRequestWithNullPhoneNumber() {
        final UpdateCustomerRequest request = new UpdateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            null);

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowValidRequestWithBlankPhoneNumber() {
        final UpdateCustomerRequest request = new UpdateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
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
        final UpdateCustomerRequest request = new UpdateCustomerRequest(
            null,
            LAST_NAME,
            VALID_PHONE);

        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void shouldThrowWhenFirstNameIsBlank() {
        final UpdateCustomerRequest request = new UpdateCustomerRequest(
            " ",
            LAST_NAME,
            VALID_PHONE);

        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void shouldThrowWhenLastNameIsNull() {
        final UpdateCustomerRequest request = new UpdateCustomerRequest(
            FIRST_NAME,
            null,
            VALID_PHONE);

        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void shouldThrowWhenLastNameIsBlank() {
        final UpdateCustomerRequest request = new UpdateCustomerRequest(
            FIRST_NAME,
            " ",
            VALID_PHONE);

        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void shouldThrowWhenPhoneNumberIsInvalid() {
        final UpdateCustomerRequest request = new UpdateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            INVALID_PHONE);

        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void shouldThrowWhenPhoneNumberIsTooShort() {
        final UpdateCustomerRequest request = new UpdateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            TOO_SHORT_PHONE);

        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void shouldThrowWhenPhoneNumberIsTooLong() {
        final UpdateCustomerRequest request = new UpdateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            TOO_LONG_PHONE);

        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(RequestValidationException.class);
    }
}
