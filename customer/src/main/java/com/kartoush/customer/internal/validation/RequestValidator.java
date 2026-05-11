package com.kartoush.customer.internal.validation;

import com.kartoush.platform.validation.RequestValidationException;
import com.kartoush.platform.validation.ValidationError;

import java.util.List;

public abstract class RequestValidator {
    protected static final String VALIDATION_MESSAGE = "Request validation failed";

    protected final void throwIfErrors(final List<ValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new RequestValidationException(VALIDATION_MESSAGE, errors);
        }
    }
}
