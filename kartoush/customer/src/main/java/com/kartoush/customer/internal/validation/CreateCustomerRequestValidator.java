package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CreateCustomerRequestValidator {

    private static final Logger LOG = LoggerFactory.getLogger(CreateCustomerRequestValidator.class);

    private static final String VALIDATION_MESSAGE = "Request validation failed";

    public void validate(final CreateCustomerRequest request) {
        LOG.debug("Validating create customer request");

        final List<ValidationError> errors = new ArrayList<>();

        validateRequest(request, errors);

        if (request == null) {
            throwIfErrors(errors);
            return;
        }

        RequestValidationSupport.validateRequiredEmail("email", request.email(), errors);
        RequestValidationSupport.validateRequiredText("firstName", request.firstName(), 100, errors);
        RequestValidationSupport.validateRequiredText("lastName", request.lastName(), 100, errors);
        RequestValidationSupport.validateOptionalPhoneNumber("phoneNUmber", request.phoneNumber(), errors);

        throwIfErrors(errors);
    }

    private void validateRequest(final CreateCustomerRequest request, final List<ValidationError> errors) {
        if (request == null) {
            errors.add(new ValidationError("request", "request must not be null"));
        }
    }

    private void throwIfErrors(final List<ValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new RequestValidationException(VALIDATION_MESSAGE, errors);
        }
    }
}
