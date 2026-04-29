package com.kartoush.customer.internal.validation;

import com.kartoush.auth.config.CustomerPasswordPolicyProperties;
import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SetInitialCustomerPasswordInputValidator {

    private static final String VALIDATION_MESSAGE = "Request validation failed";
    private final CustomerPasswordPolicyProperties passwordPolicy;

    public SetInitialCustomerPasswordInputValidator(final CustomerPasswordPolicyProperties passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public void validate(final InitialCustomerPasswordInput input) {
        final List<ValidationError> errors = new ArrayList<>();

        if (input == null) {
            errors.add(new ValidationError("input", "input must not be null"));
            throwIfErrors(errors);
            return;
        }

        RequestValidationSupport.validateRequiredText("setupToken", input.setupToken(), 512, errors);
        RequestValidationSupport.validateRequiredText("password", input.password(), passwordPolicy.getMaxLength(), errors);
        RequestValidationSupport.validateRequiredText("confirmPassword", input.confirmPassword(), passwordPolicy.getMaxLength(), errors);

        validatePasswordPolicy(input.password(), errors);

        if (input.password() != null
            && input.confirmPassword() != null
            && !input.password().equals(input.confirmPassword())) {
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
                "password must be at least " + passwordPolicy.getMinLength() + " characters"));
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
