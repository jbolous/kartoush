package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.customer.internal.registration.TermsOfServiceCatalog;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomerRegistrationValidator {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerRegistrationValidator.class);

    private static final String VALIDATION_MESSAGE = "Request validation failed";

    private final TermsOfServiceCatalog termsOfServiceCatalog;

    public CustomerRegistrationValidator(final TermsOfServiceCatalog termsOfServiceCatalog) {
        this.termsOfServiceCatalog = termsOfServiceCatalog;
    }

    public void validate(final CreateCustomerInput input) {
        LOG.debug("Validating customer registration input");

        final List<ValidationError> errors = new ArrayList<>();

        if (input == null) {
            errors.add(new ValidationError("input", "input must not be null"));
            throwIfErrors(errors);
            return;
        }

        RequestValidationSupport.validateRequiredEmail("email", input.email(), errors);
        RequestValidationSupport.validateRequiredText("firstName", input.firstName(), 100, errors);
        RequestValidationSupport.validateRequiredText("lastName", input.lastName(), 100, errors);
        RequestValidationSupport.validateOptionalPhoneNumber("phoneNumber", input.phoneNumber(), errors);
        validateTermsAcceptance(input, errors);

        throwIfErrors(errors);
    }

    private void validateTermsAcceptance(final CreateCustomerInput input, final List<ValidationError> errors) {
        if (!Boolean.TRUE.equals(input.termsAccepted())) {
            errors.add(new ValidationError("termsAccepted", "termsAccepted must be true"));
        }

        if (input.termsVersion() == null || input.termsVersion().isBlank()) {
            errors.add(new ValidationError("termsVersion", "termsVersion must not be blank"));
            return;
        }

        if (!termsOfServiceCatalog.currentVersion().equals(input.termsVersion())) {
            errors.add(new ValidationError("termsVersion", "termsVersion must match the current supported Terms of Service version"));
        }
    }

    private void throwIfErrors(final List<ValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new RequestValidationException(VALIDATION_MESSAGE, errors);
        }
    }
}
