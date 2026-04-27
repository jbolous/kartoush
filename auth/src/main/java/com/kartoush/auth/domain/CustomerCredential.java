package com.kartoush.auth.domain;

import com.kartoush.platform.types.CustomerId;

import java.util.Objects;

public record CustomerCredential(
    CustomerId customerId,
    String passwordHash
) {
    public CustomerCredential {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    }
}
