package com.kartoush.customer.internal.validation;

import com.kartoush.customer.domain.CustomerConstraints;
import com.kartoush.customer.facade.model.UpdateCustomerRequest;
import com.kartoush.platform.types.PhoneNumber;
import com.kartoush.platform.types.exception.InvalidPhoneNumberException;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UpdateCustomerRequestValidator {
    private static final String VALIDATION_MESSAGE = "Request validation failed";

    public void validate(final UpdateCustomerRequest request) {
        final List<ValidationError> errors = new ArrayList<>();

        if (request == null) {
            errors.add(new ValidationError("request", "Request must not be null"));
            throwIfErrors(errors);
            return;
        }

        RequestValidationSupport.validateRequiredText("firstName", request.firstName(), CustomerConstraints.NAME_MAX_LENGTH, errors);
        RequestValidationSupport.validateRequiredText("lastName", request.lastName(), CustomerConstraints.NAME_MAX_LENGTH, errors);
        RequestValidationSupport.validateOptionalPhoneNumber("phoneNumber", request.phoneNumber(), errors);

        throwIfErrors(errors);
    }

    private void throwIfErrors(final List<ValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new RequestValidationException(VALIDATION_MESSAGE, errors);
        }
    }
}
