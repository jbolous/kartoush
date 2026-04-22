package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.CreateCustomerCommand;
import com.kartoush.customer.internal.registration.TermsOfServiceCatalog;
import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CreateCustomerCommandValidator {

    private static final Logger LOG = LoggerFactory.getLogger(CreateCustomerCommandValidator.class);

    private static final String VALIDATION_MESSAGE = "Request validation failed";

    private final TermsOfServiceCatalog termsOfServiceCatalog;

    public CreateCustomerCommandValidator(final TermsOfServiceCatalog termsOfServiceCatalog) {
        this.termsOfServiceCatalog = termsOfServiceCatalog;
    }

    public void validate(final CreateCustomerCommand command) {
        LOG.debug("Validating create customer command");

        final List<ValidationError> errors = new ArrayList<>();

        validateCommand(command, errors);

        if (command == null) {
            throwIfErrors(errors);
            return;
        }

        RequestValidationSupport.validateRequiredEmail("email", command.email(), errors);
        RequestValidationSupport.validateRequiredText("firstName", command.firstName(), 100, errors);
        RequestValidationSupport.validateRequiredText("lastName", command.lastName(), 100, errors);
        RequestValidationSupport.validateOptionalPhoneNumber("phoneNumber", command.phoneNumber(), errors);
        validateTermsAcceptance(command, errors);

        throwIfErrors(errors);
    }

    private void validateCommand(final CreateCustomerCommand command, final List<ValidationError> errors) {
        if (command == null) {
            errors.add(new ValidationError("command", "command must not be null"));
        }
    }

    private void throwIfErrors(final List<ValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new RequestValidationException(VALIDATION_MESSAGE, errors);
        }
    }

    private void validateTermsAcceptance(final CreateCustomerCommand command, final List<ValidationError> errors) {
        if (!Boolean.TRUE.equals(command.termsAccepted())) {
            errors.add(new ValidationError("termsAccepted", "termsAccepted must be true"));
        }

        if (command.termsVersion() == null || command.termsVersion().isBlank()) {
            errors.add(new ValidationError("termsVersion", "termsVersion must not be blank"));
            return;
        }

        if (!termsOfServiceCatalog.currentVersion().equals(command.termsVersion())) {
            errors.add(new ValidationError("termsVersion", "termsVersion must match the current supported Terms of Service version"));
        }
    }
}
