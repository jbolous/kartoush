package com.kartoush.platform.validation;

import com.kartoush.platform.types.Email;
import com.kartoush.platform.types.PhoneNumber;
import com.kartoush.platform.types.exception.InvalidEmailException;
import com.kartoush.platform.types.exception.InvalidPhoneNumberException;

import java.util.ArrayList;
import java.util.List;

public final class RequestValidationSupport {

    private RequestValidationSupport() {
    }

    public static void validateRequiredText(
        final String field,
        final String value,
        final int maxLength,
        final List<ValidationError> errors) {
        if (value == null) {
            errors.add(new ValidationError(field, field + " must not be null"));
            return;
        }

        if (value.isBlank()) {
            errors.add(new ValidationError(field, field + " must not be blank"));
            return;
        }

        if (value.length() > maxLength) {
            errors.add(new ValidationError(field, field + " must not exceed " + maxLength + " characters"));
        }
    }

    public static void validateOptionalPhoneNumber(
        final String field,
        final String phoneNumber,
        final List<ValidationError> errors) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }

        try {
            new PhoneNumber(phoneNumber);
        }
        catch (final InvalidPhoneNumberException ex) {
            errors.add(new ValidationError(field, ex.getMessage()));
        }
    }

    public static void validateRequiredEmail(
        final String field,
        final String email,
        final List<ValidationError> errors) {

        if (email == null || email.isBlank()) {
            errors.add(new ValidationError(field, "Email is required"));
            return;
        }

        try {
            new Email(email);
        } catch (final InvalidEmailException exception) {
            errors.add(new ValidationError(field, exception.getMessage()));
        }
    }

    public static List<ValidationError> requireNonNullInput(final Object input) {
        final List<ValidationError> errors = new ArrayList<>();

        if (input == null) {
            errors.add(new ValidationError("input", "input must not be null"));
        }

        return errors;
    }

}
