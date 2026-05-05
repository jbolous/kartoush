package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.customer.internal.registration.TermsOfServiceCatalog;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.ValidationError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class CreateCustomerInputValidatorTest {

    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String VALID_EMAIL = "jack@kartoush.com";
    private static final String INVALID_EMAIL = "@";
    private static final String VALID_PHONE = "+16305551234";
    private static final String INVALID_PHONE = "abc123";
    private static final String CURRENT_TERMS_VERSION = "2026.04.01";
    private static final String INVALID_TERMS_VERSION = "2026-03";

    private final TermsOfServiceCatalog termsOfServiceCatalog = mock(TermsOfServiceCatalog.class);
    private final CustomerRegistrationValidator validator =
        new CustomerRegistrationValidator(termsOfServiceCatalog);

    CreateCustomerInputValidatorTest() {
        given(termsOfServiceCatalog.currentVersion()).willReturn(CURRENT_TERMS_VERSION);
    }

    @Test
    void shouldAllowValidInput() {
        final CreateCustomerInput input = new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION);

        assertThatCode(() -> validator.validate(input)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowValidInputWithNullPhoneNumber() {
        final CreateCustomerInput input = new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            null,
            true,
            CURRENT_TERMS_VERSION);

        assertThatCode(() -> validator.validate(input)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowValidInputWithBlankPhoneNumber() {
        final CreateCustomerInput input = new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            " ",
            true,
            CURRENT_TERMS_VERSION);

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
        assertValidationError(new CreateCustomerInput(
            null,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION), "firstName");
    }

    @Test
    void shouldThrowWhenFirstNameIsBlank() {
        assertValidationError(new CreateCustomerInput(
            " ",
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION), "firstName");
    }

    @Test
    void shouldThrowWhenLastNameIsNull() {
        assertValidationError(new CreateCustomerInput(
            FIRST_NAME,
            null,
            VALID_EMAIL,
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION), "lastName");
    }

    @Test
    void shouldThrowWhenLastNameIsBlank() {
        assertValidationError(new CreateCustomerInput(
            FIRST_NAME,
            " ",
            VALID_EMAIL,
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION), "lastName");
    }

    @Test
    void shouldThrowWhenEmailIsNull() {
        assertValidationError(new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            null,
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION), "email");
    }

    @Test
    void shouldThrowWhenEmailIsBlank() {
        assertValidationError(new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            " ",
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION), "email");
    }

    @Test
    void shouldThrowWhenEmailIsInvalid() {
        assertValidationError(new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            INVALID_EMAIL,
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION), "email");
    }

    @Test
    void shouldThrowWhenPhoneNumberIsInvalid() {
        assertValidationError(new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            INVALID_PHONE,
            true,
            CURRENT_TERMS_VERSION), "phoneNumber");
    }

    @Test
    void shouldThrowWhenTermsAcceptedIsMissing() {
        assertValidationError(new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            null,
            CURRENT_TERMS_VERSION), "termsAccepted");
    }

    @Test
    void shouldThrowWhenTermsAcceptedIsFalse() {
        assertValidationError(new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            false,
            CURRENT_TERMS_VERSION), "termsAccepted");
    }

    @Test
    void shouldThrowWhenTermsVersionIsMissing() {
        assertValidationError(new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            true,
            null), "termsVersion");
    }

    @Test
    void shouldThrowWhenTermsVersionIsBlank() {
        assertValidationError(new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            true,
            " "), "termsVersion");
    }

    @Test
    void shouldThrowWhenTermsVersionDoesNotMatchCurrentVersion() {
        assertValidationError(new CreateCustomerInput(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            true,
            INVALID_TERMS_VERSION), "termsVersion");
    }

    private void assertValidationError(final CreateCustomerInput input, final String field) {
        assertThatThrownBy(() -> validator.validate(input))
            .isInstanceOfSatisfying(RequestValidationException.class, exception -> {
                assertThat(exception.getErrors())
                    .extracting(ValidationError::field)
                    .contains(field);
            });
    }
}
