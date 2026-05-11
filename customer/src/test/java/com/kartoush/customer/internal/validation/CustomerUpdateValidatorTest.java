package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.UpdateCustomerInput;
import com.kartoush.platform.validation.RequestValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerUpdateValidatorTest {
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String VALID_PHONE = "+16305551234";
    private static final String INVALID_PHONE = "abc123";
    private static final String TOO_SHORT_PHONE = "123456";
    private static final String TOO_LONG_PHONE = "+1234567890123456";

    private final CustomerUpdateValidator validator = new CustomerUpdateValidator();

    @Test
    void shouldAllowValidInput() {
        final UpdateCustomerInput input = new UpdateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            VALID_PHONE);

        assertThatCode(() -> validator.validate(input)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowValidInputWithNullPhoneNumber() {
        final UpdateCustomerInput input = new UpdateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            null);

        assertThatCode(() -> validator.validate(input)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowValidInputWithBlankPhoneNumber() {
        final UpdateCustomerInput input = new UpdateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            " ");

        assertThatCode(() -> validator.validate(input)).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenInputIsNull() {
        assertThatThrownBy(() -> validator.validate(null))
            .isInstanceOf(RequestValidationException.class)
            .hasMessage("Request validation failed");
    }

    @Test
    void shouldThrowWhenFirstNameIsNull() {
        assertThatThrownBy(() -> validator.validate(new UpdateCustomerInput(
            null,
            LAST_NAME,
            VALID_PHONE)))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void shouldThrowWhenFirstNameIsBlank() {
        assertThatThrownBy(() -> validator.validate(new UpdateCustomerInput(
            " ",
            LAST_NAME,
            VALID_PHONE)))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void shouldThrowWhenLastNameIsNull() {
        assertThatThrownBy(() -> validator.validate(new UpdateCustomerInput(
            FIRST_NAME,
            null,
            VALID_PHONE)))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void shouldThrowWhenLastNameIsBlank() {
        assertThatThrownBy(() -> validator.validate(new UpdateCustomerInput(
            FIRST_NAME,
            " ",
            VALID_PHONE)))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void shouldThrowWhenPhoneNumberIsInvalid() {
        assertThatThrownBy(() -> validator.validate(new UpdateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            INVALID_PHONE)))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void shouldThrowWhenPhoneNumberIsTooShort() {
        assertThatThrownBy(() -> validator.validate(new UpdateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            TOO_SHORT_PHONE)))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void shouldThrowWhenPhoneNumberIsTooLong() {
        assertThatThrownBy(() -> validator.validate(new UpdateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            TOO_LONG_PHONE)))
            .isInstanceOf(RequestValidationException.class);
    }
}
