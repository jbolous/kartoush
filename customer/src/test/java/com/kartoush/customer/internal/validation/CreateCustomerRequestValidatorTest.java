package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.internal.registration.TermsOfServiceCatalog;
import com.kartoush.platform.validation.RequestValidationException;
import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
    private static final String CURRENT_TERMS_VERSION = "2026.04.01";
    private static final String INVALID_TERMS_VERSION = "2026-03";

    private final TermsOfServiceCatalog termsOfServiceCatalog = mock(TermsOfServiceCatalog.class);
    private final CreateCustomerRequestValidator validator =
        new CreateCustomerRequestValidator(termsOfServiceCatalog);

    CreateCustomerRequestValidatorTest() {
        given(termsOfServiceCatalog.currentVersion()).willReturn(CURRENT_TERMS_VERSION);
    }

    @Test
    void shouldAllowValidRequest() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION);

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowValidRequestWithNullPhoneNumber() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            null,
            true,
            CURRENT_TERMS_VERSION);

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowValidRequestWithBlankPhoneNumber() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            " ",
            true,
            CURRENT_TERMS_VERSION);

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
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION);

        assertValidationError(request, "firstName");
    }

    @Test
    void shouldThrowWhenFirstNameIsBlank() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            " ",
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION);

        assertValidationError(request, "firstName");
    }

    @Test
    void shouldThrowWhenLastNameIsNull() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            null,
            VALID_EMAIL,
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION);

        assertValidationError(request, "lastName");
    }

    @Test
    void shouldThrowWhenLastNameIsBlank() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            " ",
            VALID_EMAIL,
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION);

        assertValidationError(request, "lastName");
    }

    @Test
    void shouldThrowWhenEmailIsNull() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            null,
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION);

        assertValidationError(request, "email");
    }

    @Test
    void shouldThrowWhenEmailIsBlank() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            " ",
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION);

        assertValidationError(request, "email");
    }

    @Test
    void shouldThrowWhenEmailIsInvalid() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            INVALID_EMAIL,
            VALID_PHONE,
            true,
            CURRENT_TERMS_VERSION);

        assertValidationError(request, "email");
    }

    @Test
    void shouldThrowWhenPhoneNumberIsInvalid() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            INVALID_PHONE,
            true,
            CURRENT_TERMS_VERSION);

        assertValidationError(request, "phoneNumber");
    }

    @Test
    void shouldThrowWhenTermsAcceptedIsMissing() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            null,
            CURRENT_TERMS_VERSION);

        assertValidationError(request, "termsAccepted");
    }

    @Test
    void shouldThrowWhenTermsAcceptedIsFalse() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            false,
            CURRENT_TERMS_VERSION);

        assertValidationError(request, "termsAccepted");
    }

    @Test
    void shouldThrowWhenTermsVersionIsMissing() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            true,
            null);

        assertValidationError(request, "termsVersion");
    }

    @Test
    void shouldThrowWhenTermsVersionIsBlank() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            true,
            " ");

        assertValidationError(request, "termsVersion");
    }

    @Test
    void shouldThrowWhenTermsVersionDoesNotMatchCurrentVersion() {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            VALID_EMAIL,
            VALID_PHONE,
            true,
            INVALID_TERMS_VERSION);

        assertValidationError(request, "termsVersion");
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
