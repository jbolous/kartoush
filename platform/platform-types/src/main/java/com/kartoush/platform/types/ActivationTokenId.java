package com.kartoush.platform.types;

import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.platform.ulid.Ulids;

import java.util.Objects;

public class ActivationTokenId {
    private final String value;

    private ActivationTokenId(String value) {
        this.value = value;
    }

    public static ActivationTokenId of(String value) {
        return new ActivationTokenId(
            Ulids.requireValid(value, "ActivationTokenId")
        );
    }

    public static ActivationTokenId newId(UlidGenerator generator) {
        return new ActivationTokenId(generator.next());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ActivationTokenId that = (ActivationTokenId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
