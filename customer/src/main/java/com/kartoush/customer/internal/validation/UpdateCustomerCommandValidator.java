package com.kartoush.customer.internal.validation;

import com.kartoush.customer.domain.CustomerConstraints;
import com.kartoush.customer.facade.model.UpdateCustomerCommand;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UpdateCustomerCommandValidator {
    private static final String VALIDATION_MESSAGE = "Request validation failed";

    public void validate(final UpdateCustomerCommand command) {
        final List<ValidationError> errors = new ArrayList<>();

        if (command == null) {
            errors.add(new ValidationError("command", "command must not be null"));
            throwIfErrors(errors);
            return;
        }

        RequestValidationSupport.validateRequiredText("firstName", command.firstName(), CustomerConstraints.NAME_MAX_LENGTH, errors);
        RequestValidationSupport.validateRequiredText("lastName", command.lastName(), CustomerConstraints.NAME_MAX_LENGTH, errors);
        RequestValidationSupport.validateOptionalPhoneNumber("phoneNumber", command.phoneNumber(), errors);

        throwIfErrors(errors);
    }

    private void throwIfErrors(final List<ValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new RequestValidationException(VALIDATION_MESSAGE, errors);
        }
    }
}
