package com.kartoush.api.auth;

import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;
import com.kartoush.platform.validation.password.PasswordPolicyValidator;
import com.kartoush.platform.validation.password.PasswordValidationSupport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ResetPasswordRequestValidator {

    private static final String VALIDATION_MESSAGE = "Request validation failed";

    private final PasswordPolicyValidator passwordPolicyValidator;

    public ResetPasswordRequestValidator(final PasswordPolicyValidator passwordPolicyValidator) {
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    public void validate(final ResetPasswordRequest request) {
        final List<ValidationError> errors = new ArrayList<>();

        if (request == null) {
            errors.add(new ValidationError("input", "input must not be null"));
            throwIfErrors(errors);
            return;
        }

        RequestValidationSupport.validateRequiredText("resetToken", request.resetToken(), 512, errors);

        PasswordValidationSupport.validatePasswordWithConfirmation(
            request.password(),
            request.confirmPassword(),
            passwordPolicyValidator,
            errors);

        throwIfErrors(errors);
    }

    private void throwIfErrors(final List<ValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new RequestValidationException(VALIDATION_MESSAGE, errors);
        }
    }
}
