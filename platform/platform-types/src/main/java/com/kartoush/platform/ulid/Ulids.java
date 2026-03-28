package com.kartoush.platform.ulid;

import com.github.f4b6a3.ulid.Ulid;

public final class Ulids {

    private Ulids() {
        // prevent instantiation
    }

    public static String requireValid(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidUlidException(fieldName + " must not be blank");
        }

        try {
            // parse + validate
            Ulid ulid = Ulid.from(value);

            // return canonical 26-char uppercase representation
            return ulid.toString();

        } catch (RuntimeException ex) {
            throw new InvalidUlidException(
                    fieldName + " is not a valid ULID: " + value,
                    ex
            );
        }
    }
}
