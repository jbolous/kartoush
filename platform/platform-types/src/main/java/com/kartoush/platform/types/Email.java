package com.kartoush.platform.types;

import com.kartoush.platform.types.exception.InvalidEmailException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final int MAX_LENGTH = 150;

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final String DELETED_SUFFIX = "|deleted|";

    public Email {
        if (value == null) {
            throw new InvalidEmailException("Email must not be null");
        }
        value = normalize(value);

        if (value.isBlank()) {
            throw new InvalidEmailException("Email must not be blank");
        }

        if (value.length() > MAX_LENGTH) {
            throw new InvalidEmailException("Email cannot be longer than " + MAX_LENGTH);
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new InvalidEmailException("Email must be a valid email address");
        }
    }

    public Email updateForDeletion(final CustomerId customerId) {
        final String suffix = DELETED_SUFFIX + customerId.value();

        if (this.value.endsWith(suffix)) {
            return this;
        }

        return new Email(this.value + suffix);
    }

    private static String normalize(final String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
