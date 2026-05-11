package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.password.PasswordPolicyValidator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerPasswordSetupValidatorTest {

    private static final String VALID_SETUP_TOKEN = "setup-token";
    private static final String VALID_PASSWORD = "Password123!";

    private final CustomerPasswordSetupValidator validator =
        new CustomerPasswordSetupValidator(new TestPasswordPolicyValidator());

    @Test
    void shouldAllowPasswordThatMatchesDefaultPolicy() {
        final InitialCustomerPasswordInput input =
            new InitialCustomerPasswordInput(VALID_SETUP_TOKEN, VALID_PASSWORD, VALID_PASSWORD);

        assertThatCode(() -> validator.validate(input)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectPasswordThatIsTooShort() {
        assertValidationError(
            new InitialCustomerPasswordInput(VALID_SETUP_TOKEN, "Pass1!", "Pass1!"),
            "password",
            "password must be at least 12 characters"
        );
    }

    @Test
    void shouldRejectPasswordWithoutUppercaseLetter() {
        assertValidationError(
            new InitialCustomerPasswordInput(VALID_SETUP_TOKEN, "password123!", "password123!"),
            "password",
            "password must contain at least one uppercase letter"
        );
    }

    @Test
    void shouldRejectPasswordWithoutLowercaseLetter() {
        assertValidationError(
            new InitialCustomerPasswordInput(VALID_SETUP_TOKEN, "PASSWORD123!", "PASSWORD123!"),
            "password",
            "password must contain at least one lowercase letter"
        );
    }

    @Test
    void shouldRejectPasswordWithoutDigit() {
        assertValidationError(
            new InitialCustomerPasswordInput(VALID_SETUP_TOKEN, "PasswordOnly!", "PasswordOnly!"),
            "password",
            "password must contain at least one digit"
        );
    }

    @Test
    void shouldRejectPasswordWithoutSpecialCharacter() {
        assertValidationError(
            new InitialCustomerPasswordInput(VALID_SETUP_TOKEN, "Password1234", "Password1234"),
            "password",
            "password must contain at least one special character"
        );
    }

    @Test
    void shouldRejectConfirmPasswordThatDoesNotMatch() {
        assertValidationError(
            new InitialCustomerPasswordInput(VALID_SETUP_TOKEN, VALID_PASSWORD, "Password1234!"),
            "confirmPassword",
            "confirmPassword must match password"
        );
    }

    private void assertValidationError(
        final InitialCustomerPasswordInput input,
        final String field,
        final String message
    ) {
        assertThatThrownBy(() -> validator.validate(input))
            .isInstanceOfSatisfying(RequestValidationException.class, exception -> {
                assertThat(exception.getErrors())
                    .anySatisfy(error -> {
                        assertThat(error.field()).isEqualTo(field);
                        assertThat(error.message()).isEqualTo(message);
                    });
            });
    }

    private static final class TestPasswordPolicyValidator implements PasswordPolicyValidator {

        @Override
        public int maxLength() {
            return 128;
        }

        @Override
        public List<String> validatePassword(final String password) {
            final List<String> messages = new ArrayList<>();

            if (password == null || password.isBlank()) {
                return messages;
            }

            if (password.length() < 12) {
                messages.add("must be at least 12 characters");
            }

            if (password.chars().noneMatch(Character::isUpperCase)) {
                messages.add("must contain at least one uppercase letter");
            }

            if (password.chars().noneMatch(Character::isLowerCase)) {
                messages.add("must contain at least one lowercase letter");
            }

            if (password.chars().noneMatch(Character::isDigit)) {
                messages.add("must contain at least one digit");
            }

            if (password.chars().allMatch(Character::isLetterOrDigit)) {
                messages.add("must contain at least one special character");
            }

            return messages;
        }
    }
}
