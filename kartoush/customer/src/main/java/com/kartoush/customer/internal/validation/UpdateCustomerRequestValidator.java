package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.UpdateCustomerRequest;
import com.kartoush.platform.types.PhoneNumber;
import com.kartoush.platform.types.exception.InvalidPhoneNumberException;
import com.kartoush.platform.validation.RequestValidationException;
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

        validateRequiredText("firstName", request.firstName(), 100, errors);
        validateRequiredText("lastName", request.lastName(), 100, errors);
        validateOptionalPhoneNumber(request.phoneNumber(), errors);

        throwIfErrors(errors);
    }

    private void validateRequiredText(
            final String field,
            final String value,
            final int maxLength,
            final List<ValidationError> errors) {
        if (value == null) {
            errors.add(new ValidationError(field, field + " must not be null"));
            return;
        }

        if (value.isBlank()) {
            errors.add(new ValidationError(field, field + " must not be blank"));
            return;
        }

        if (value.length() > maxLength) {
            errors.add(new ValidationError(field, field + " must not exceed " + maxLength + " characters"));
        }
    }

    private void validateOptionalPhoneNumber(
            final String phoneNumber,
            final List<ValidationError> errors) {
        if (phoneNumber == null) {
            return;
        }

        try {
            new PhoneNumber(phoneNumber);
        }
        catch (final InvalidPhoneNumberException ex) {
            errors.add(new ValidationError("phoneNumber", ex.getMessage()));
        }
    }

    private void throwIfErrors(final List<ValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new RequestValidationException(VALIDATION_MESSAGE, errors);
        }
    }
}
