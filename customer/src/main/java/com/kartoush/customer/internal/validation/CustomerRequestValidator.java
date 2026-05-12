package com.kartoush.customer.internal.validation;

import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.platform.validation.RequestValidationSupport;
import com.kartoush.platform.validation.ValidationError;

import java.util.List;

public abstract class CustomerRequestValidator extends RequestValidator {

    protected void validateCustomer(CreateCustomerInput input, List<ValidationError> errors) {

        RequestValidationSupport.validateRequiredEmail("email", input.email(), errors);
        RequestValidationSupport.validateRequiredText("firstName", input.firstName(), 100, errors);
        RequestValidationSupport.validateRequiredText("lastName", input.lastName(), 100, errors);
        RequestValidationSupport.validateOptionalPhoneNumber("phoneNumber", input.phoneNumber(), errors);
    }
}
