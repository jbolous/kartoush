package com.kartoush.platform.types;

import com.kartoush.platform.types.exception.InvalidPhoneNumberException;

import java.util.regex.Pattern;

public record PhoneNumber(String value)
{
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^\\+?[0-9]{7,15}$");

    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw new InvalidPhoneNumberException("Phone number must not be blank");
        }

        if (!PHONE_NUMBER_PATTERN.matcher(value).matches()) {
            throw new InvalidPhoneNumberException("Phone number must be a valid phone number");
        }
    }
}
