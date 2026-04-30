package com.kartoush.auth.domain;

import com.kartoush.platform.types.CustomerId;

import java.time.Instant;
import java.util.Objects;

public record PasswordResetToken(
    String id,
    CustomerId customerId,
    String tokenHash,
    Instant expiresAt,
    Instant consumedAt,
    Instant createdAt
) {

    public PasswordResetToken {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isExpired(final Instant now) {
        return !expiresAt.isAfter(now);
    }

    public PasswordResetToken consume(final Instant consumedAt) {
        if (isConsumed()) {
            return this;
        }

        return new PasswordResetToken(
            id,
            customerId,
            tokenHash,
            expiresAt,
            Objects.requireNonNull(consumedAt, "consumedAt must not be null"),
            createdAt
        );
    }
}
