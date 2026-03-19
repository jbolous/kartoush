package com.kartoush.platform.validation;

public record ValidationError(
        String field,
        String message,
        String code,
        Object rejectedValue) {

    public ValidationError(final String field, final String message) {
        this(field, message, null, null);
    }
}
