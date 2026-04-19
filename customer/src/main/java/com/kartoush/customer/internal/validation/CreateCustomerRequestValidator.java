package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.internal.registration.TermsOfServicePolicy;
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

    private final TermsOfServicePolicy termsOfServicePolicy;

    public CreateCustomerRequestValidator(final TermsOfServicePolicy termsOfServicePolicy) {
        this.termsOfServicePolicy = termsOfServicePolicy;
    }

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
        RequestValidationSupport.validateOptionalPhoneNumber("phoneNumber", request.phoneNumber(), errors);
        validateTermsAcceptance(request, errors);

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

    private void validateTermsAcceptance(final CreateCustomerRequest request, final List<ValidationError> errors) {
        if (!Boolean.TRUE.equals(request.termsAccepted())) {
            errors.add(new ValidationError("termsAccepted", "termsAccepted must be true"));
        }

        if (request.termsVersion() == null || request.termsVersion().isBlank()) {
            errors.add(new ValidationError("termsVersion", "termsVersion must not be blank"));
            return;
        }

        if (!termsOfServicePolicy.currentVersion().equals(request.termsVersion())) {
            errors.add(new ValidationError("termsVersion", "termsVersion must match the current supported Terms of Service version"));
        }
    }
}
