package com.kartoush.api.auth;

import com.kartoush.auth.config.CustomerPasswordPolicyProperties;
import com.kartoush.auth.validation.CustomerPasswordPolicyValidator;
import com.kartoush.platform.validation.RequestValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResetPasswordRequestValidatorTest {

    private static final String VALID_EMAIL = "jack@example.com";
    private static final String VALID_TOKEN = "reset-token";
    private static final String VALID_PASSWORD = "Password123!";

    private final ResetPasswordRequestValidator validator =
        new ResetPasswordRequestValidator(
            new CustomerPasswordPolicyValidator(new CustomerPasswordPolicyProperties())
        );

    @Test
    void shouldAllowPasswordThatMatchesDefaultPolicy() {
        assertThatCode(() -> validator.validate(
            new ResetPasswordRequest(VALID_EMAIL, VALID_TOKEN, VALID_PASSWORD, VALID_PASSWORD)
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectPasswordThatDoesNotMatchPolicy() {
        assertThatThrownBy(() -> validator.validate(
            new ResetPasswordRequest(VALID_EMAIL, VALID_TOKEN, "password", "password")
        )).isInstanceOf(RequestValidationException.class);
    }
}
