package com.kartoush.customer.internal.validation;

import com.kartoush.customer.domain.CustomerConstraints;
import com.kartoush.customer.facade.model.UpdateCustomerInput;
import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomerUpdateValidator extends RequestValidator {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerUpdateValidator.class);

    public void validate(final UpdateCustomerInput input) {
        LOG.debug("Validating customer update input");

        final List<ValidationError> errors = new ArrayList<>();

        if (input == null) {
            errors.add(new ValidationError("input", "input must not be null"));
            throwIfErrors(errors);
            return;
        }

        RequestValidationSupport.validateRequiredText(
            "firstName",
            input.firstName(),
            CustomerConstraints.NAME_MAX_LENGTH,
            errors
        );

        RequestValidationSupport.validateRequiredText(
            "lastName",
            input.lastName(),
            CustomerConstraints.NAME_MAX_LENGTH,
            errors
        );

        RequestValidationSupport.validateOptionalPhoneNumber("phoneNumber", input.phoneNumber(), errors);

        throwIfErrors(errors);
    }
}
