package com.kartoush.platform.types;

import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.platform.ulid.Ulids;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CustomerId that = (CustomerId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
