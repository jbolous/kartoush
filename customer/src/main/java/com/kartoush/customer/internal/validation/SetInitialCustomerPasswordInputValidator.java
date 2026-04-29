package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SetInitialCustomerPasswordInputValidator {

    private static final int PASSWORD_MAX_LENGTH = 255;
    private static final String VALIDATION_MESSAGE = "Request validation failed";

    public void validate(final InitialCustomerPasswordInput input) {
        final List<ValidationError> errors = new ArrayList<>();

        if (input == null) {
            errors.add(new ValidationError("input", "input must not be null"));
            throwIfErrors(errors);
            return;
        }

        RequestValidationSupport.validateRequiredText("setupToken", input.setupToken(), 512, errors);
        RequestValidationSupport.validateRequiredText("password", input.password(), PASSWORD_MAX_LENGTH, errors);
        RequestValidationSupport.validateRequiredText("confirmPassword", input.confirmPassword(), PASSWORD_MAX_LENGTH, errors);

        if (input.password() != null
            && input.confirmPassword() != null
            && !input.password().equals(input.confirmPassword())) {
            errors.add(new ValidationError("confirmPassword", "confirmPassword must match password"));
        }

        throwIfErrors(errors);
    }

    private void throwIfErrors(final List<ValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new RequestValidationException(VALIDATION_MESSAGE, errors);
        }
    }
}
