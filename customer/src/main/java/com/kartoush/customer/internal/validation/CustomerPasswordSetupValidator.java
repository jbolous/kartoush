package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;
import com.kartoush.platform.validation.password.PasswordPolicyValidator;
import com.kartoush.platform.validation.password.PasswordValidationSupport;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomerPasswordSetupValidator extends RequestValidator {

    private final PasswordPolicyValidator passwordPolicyValidator;

    public CustomerPasswordSetupValidator(final PasswordPolicyValidator passwordPolicyValidator) {
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    public void validate(final InitialCustomerPasswordInput input) {
        final List<ValidationError> errors = RequestValidationSupport.requireNonNullInput(input);

        if (!errors.isEmpty()) {
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

}
