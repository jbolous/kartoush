package com.kartoush.platform.types;

import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.platform.ulid.Ulids;

public final class CustomerId {

    private final String value;

    private CustomerId(String value) {
        this.value = value;
    }

    public static CustomerId of(String value) {
        return new CustomerId(
                Ulids.requireValid(value, "CustomerId")
        );
    }

    public static CustomerId newId(UlidGenerator generator) {
        return new CustomerId(generator.next());
    }

    public String value() {
        return value;
    }
}
