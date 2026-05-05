package com.kartoush.auth.validation;

import com.kartoush.auth.config.CustomerPasswordPolicyProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerPasswordPolicyValidatorTest {

    private final CustomerPasswordPolicyValidator validator =
        new CustomerPasswordPolicyValidator(new CustomerPasswordPolicyProperties());

    @Test
    void shouldExposeConfiguredMaxLength() {
        assertThat(validator.maxLength()).isEqualTo(255);
    }

    @Test
    void shouldReturnPasswordPolicyMessages() {
        final List<String> messages = validator.validatePassword("password");

        assertThat(messages)
            .contains(
                "must be at least 12 characters",
                "must contain at least one uppercase letter",
                "must contain at least one digit",
                "must contain at least one special character"
            );
    }
}
