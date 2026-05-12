package com.kartoush.customer.domain;

import com.kartoush.platform.types.ActivationTokenId;
import com.kartoush.platform.types.CustomerId;

import java.time.Instant;
import java.util.Objects;

public class ActivationToken {

    private final ActivationTokenId id;

    private final CustomerId customerId;

    private final String tokenHash;

    private final Instant expiresAt;

    private final Instant consumedAt;

    private final Instant createdAt;

    protected ActivationToken(
        final ActivationTokenId id,
        final CustomerId customerId,
        final String tokenHash,
        final Instant expiresAt,
        final Instant consumedAt,
        final Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
        this.createdAt = createdAt;
    }

    public static ActivationToken fromPersistence(
        final ActivationTokenId id,
        final CustomerId customerId,
        final String tokenHash,
        final Instant expiresAt,
        final Instant consumedAt,
        final Instant createdAt) {
        return new ActivationToken(
            id,
            customerId,
            tokenHash,
            expiresAt,
            consumedAt,
            createdAt
        );
    }

    public static ActivationToken of(
        final ActivationTokenId id,
        final CustomerId customerId,
        final String tokenHash,
        final Instant expiresAt,
        final Instant consumedAt,
        final Instant createdAt
    ) {
        return new ActivationToken(
            id,
            customerId,
            tokenHash,
            expiresAt,
            consumedAt,
            createdAt
        );
    }


    public ActivationTokenId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isExpired(final Instant now) {
        return !expiresAt.isAfter(now);
    }

    public ActivationToken consume(final Instant consumedAt) {
        if (isConsumed()) {
            return this;
        }

        return new ActivationToken(
            id,
            customerId,
            tokenHash,
            expiresAt,
            Objects.requireNonNull(consumedAt, "consumedAt must not be null"),
            createdAt
        );
    }
}
