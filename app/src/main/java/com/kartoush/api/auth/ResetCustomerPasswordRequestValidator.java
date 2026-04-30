package com.kartoush.api.auth;

import com.kartoush.auth.config.CustomerPasswordPolicyProperties;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ResetCustomerPasswordRequestValidator {

    private static final String VALIDATION_MESSAGE = "Request validation failed";

    private final CustomerPasswordPolicyProperties passwordPolicy;

    public ResetCustomerPasswordRequestValidator(final CustomerPasswordPolicyProperties passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public void validate(final ResetCustomerPasswordRequest request) {
        final List<ValidationError> errors = new ArrayList<>();

        if (request == null) {
            errors.add(new ValidationError("input", "input must not be null"));
            throwIfErrors(errors);
            return;
        }

        RequestValidationSupport.validateRequiredText("resetToken", request.resetToken(), 512, errors);
        RequestValidationSupport.validateRequiredText("password", request.password(), passwordPolicy.getMaxLength(), errors);
        RequestValidationSupport.validateRequiredText(
            "confirmPassword",
            request.confirmPassword(),
            passwordPolicy.getMaxLength(),
            errors
        );

        validatePasswordPolicy(request.password(), errors);

        if (request.password() != null
            && request.confirmPassword() != null
            && !request.password().equals(request.confirmPassword())) {
            errors.add(new ValidationError("confirmPassword", "confirmPassword must match password"));
        }

        throwIfErrors(errors);
    }

    private void validatePasswordPolicy(final String password, final List<ValidationError> errors) {
        if (password == null || password.isBlank()) {
            return;
        }

        if (password.length() < passwordPolicy.getMinLength()) {
            errors.add(new ValidationError(
                "password",
                "password must be at least " + passwordPolicy.getMinLength() + " characters"
            ));
        }

        if (passwordPolicy.isRequireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
            errors.add(new ValidationError("password", "password must contain at least one uppercase letter"));
        }

        if (passwordPolicy.isRequireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
            errors.add(new ValidationError("password", "password must contain at least one lowercase letter"));
        }

        if (passwordPolicy.isRequireDigit() && password.chars().noneMatch(Character::isDigit)) {
            errors.add(new ValidationError("password", "password must contain at least one digit"));
        }

        if (passwordPolicy.isRequireSpecialCharacter() && password.chars().allMatch(Character::isLetterOrDigit)) {
            errors.add(new ValidationError("password", "password must contain at least one special character"));
        }
    }

    private void throwIfErrors(final List<ValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new RequestValidationException(VALIDATION_MESSAGE, errors);
        }
    }
}
