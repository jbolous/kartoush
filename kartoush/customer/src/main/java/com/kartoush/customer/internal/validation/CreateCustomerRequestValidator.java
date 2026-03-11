package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.ValidationError;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CreateCustomerRequestValidator {

    private static final String VALIDATION_MESSAGE = "Request validation failed";
    private static final String PHONE_NUMBER_PATTERN = "^\\+?[0-9]{7,15}$";

    public void validate(final CreateCustomerRequest request) {
        final List<ValidationError> errors = new ArrayList<>();

        validateRequest(request, errors);

        if (request == null) {
            throwIfErrors(errors);
            return;
        }

        validateEmail(request, errors);
        validateFirstName(request, errors);
        validateLastName(request, errors);
        validatePhoneNumber(request, errors);

        throwIfErrors(errors);
    }

    private void validateRequest(final CreateCustomerRequest request, final List<ValidationError> errors) {
        if (request == null) {
            errors.add(new ValidationError("request", "request must not be null"));
        }
    }

    private void validateEmail(final CreateCustomerRequest request, final List<ValidationError> errors) {
        if (request.email() == null || request.email().isBlank()) {
            errors.add(new ValidationError("email", "email must not be blank"));
            return;
        }

        if (!looksLikeEmail(request.email())) {
            errors.add(new ValidationError("email", "email must be a valid email address"));
        }
    }

    private void validateFirstName(final CreateCustomerRequest request, final List<ValidationError> errors) {
        requireNonBlank(request.firstName(), "firstName", errors);
    }

    private void validateLastName(final CreateCustomerRequest request, final List<ValidationError> errors) {
        requireNonBlank(request.lastName(), "lastName", errors);
    }

    private void validatePhoneNumber(final CreateCustomerRequest request, final List<ValidationError> errors) {
        final String phoneNumber = request.phoneNumber();

        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }

        if (!phoneNumber.matches(PHONE_NUMBER_PATTERN)) {
            errors.add(new ValidationError(
                    "phoneNumber",
                    "phoneNumber must contain only digits and may optionally start with +"));
        }
    }

    private void requireNonBlank(final String value, final String field, final List<ValidationError> errors) {
        if (value == null || value.isBlank()) {
            errors.add(new ValidationError(field, field + " must not be blank"));
        }
    }

    private boolean looksLikeEmail(final String email) {
        final int atIndex = email.indexOf('@');

        return atIndex > 0 && atIndex < email.length() - 1;
    }

    private void throwIfErrors(final List<ValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new RequestValidationException(VALIDATION_MESSAGE, errors);
        }
    }
}
