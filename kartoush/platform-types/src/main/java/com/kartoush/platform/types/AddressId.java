package com.kartoush.platform.types;

import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.platform.ulid.Ulids;

import java.util.Objects;

public final class AddressId {

    private final String value;

    private AddressId(String value) {
        this.value = Ulids.requireValid(value, "AddressId");
    }

    public static AddressId of(String value) {
        return new AddressId(value);
    }

    public static AddressId newId(UlidGenerator generator) {
        Objects.requireNonNull(generator, "generator is required");
        return new AddressId(generator.next());
    }

    public String value() {
        return value;
    }
}
