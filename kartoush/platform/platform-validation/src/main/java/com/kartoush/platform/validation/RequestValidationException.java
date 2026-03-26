package com.kartoush.platform.validation;

import java.util.List;

public class RequestValidationException extends RuntimeException {

    private final List<ValidationError> errors;

    public RequestValidationException(final String message, final List<ValidationError> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}
