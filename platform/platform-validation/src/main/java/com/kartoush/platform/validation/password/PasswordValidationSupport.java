package com.kartoush.platform.validation.password;

import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;
import java.util.List;

public final class PasswordValidationSupport {

    private static final String PASSWORD_FIELD = "password";
    private static final String CONFIRM_PASSWORD_FIELD = "confirmPassword";

    private PasswordValidationSupport() {
    }

    public static void validatePasswordWithConfirmation(
        final String password,
        final String confirmPassword,
        final PasswordPolicyValidator passwordPolicyValidator,
        final List<ValidationError> errors
    ) {
        RequestValidationSupport.validateRequiredText(
            PASSWORD_FIELD,
            password,
            passwordPolicyValidator.maxLength(),
            errors
        );

        RequestValidationSupport.validateRequiredText(
            CONFIRM_PASSWORD_FIELD,
            confirmPassword,
            passwordPolicyValidator.maxLength(),
            errors
        );

        passwordPolicyValidator.validatePassword(password)
            .forEach(message -> errors.add(new ValidationError(PASSWORD_FIELD, PASSWORD_FIELD + " " + message)));

        if (password != null && confirmPassword != null && !password.equals(confirmPassword)) {
            errors.add(new ValidationError(CONFIRM_PASSWORD_FIELD, CONFIRM_PASSWORD_FIELD + " must match " + PASSWORD_FIELD));
        }
    }
}
