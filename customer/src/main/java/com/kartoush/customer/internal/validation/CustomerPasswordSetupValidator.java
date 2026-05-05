package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;
import com.kartoush.platform.validation.password.PasswordPolicyValidator;
import com.kartoush.platform.validation.password.PasswordValidationSupport;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CustomerPasswordSetupValidator {

    private static final String VALIDATION_MESSAGE = "Request validation failed";

    private final PasswordPolicyValidator passwordPolicyValidator;

    public CustomerPasswordSetupValidator(final PasswordPolicyValidator passwordPolicyValidator) {
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    public void validate(final InitialCustomerPasswordInput input) {
        final List<ValidationError> errors = new ArrayList<>();

        if (input == null) {
            errors.add(new ValidationError("input", "input must not be null"));
            throwIfErrors(errors);
            return;
        }

        RequestValidationSupport.validateRequiredText("setupToken", input.setupToken(), 512, errors);

        PasswordValidationSupport.validatePasswordWithConfirmation(
            input.password(),
            input.confirmPassword(),
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
