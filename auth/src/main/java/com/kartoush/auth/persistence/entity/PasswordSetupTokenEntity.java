package com.kartoush.auth.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "customer_password_setup_token")
public class PasswordSetupTokenEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 26)
    private String id;

    @Column(name = "customer_id", nullable = false, length = 26)
    private String customerId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PasswordSetupTokenEntity() {
    }

    private PasswordSetupTokenEntity(
        final String id,
        final String customerId,
        final String tokenHash,
        final Instant expiresAt,
        final Instant consumedAt,
        final Instant createdAt
    ) {
        this.id = requireNonBlank(id, "id is required");
        this.customerId = requireNonBlank(customerId, "customerId is required");
        this.tokenHash = requireNonBlank(tokenHash, "tokenHash is required");
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
        this.createdAt = createdAt;
    }

    public static PasswordSetupTokenEntity create(
        final String id,
        final String customerId,
        final String tokenHash,
        final Instant expiresAt,
        final Instant consumedAt,
        final Instant createdAt
    ) {
        return new PasswordSetupTokenEntity(id, customerId, tokenHash, expiresAt, consumedAt, createdAt);
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
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

    public void setConsumedAt(final Instant consumedAt) {
        this.consumedAt = consumedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireNonBlank(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}
