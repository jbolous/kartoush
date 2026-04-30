package com.kartoush.auth.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "customer_auth_session")
public class CustomerAuthSessionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 26)
    private String id;

    @Column(name = "customer_id", nullable = false, length = 26)
    private String customerId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected CustomerAuthSessionEntity() {
    }

    private CustomerAuthSessionEntity(
        final String id,
        final String customerId,
        final String tokenHash,
        final Instant issuedAt,
        final Instant revokedAt
    ) {
        this.id = requireNonBlank(id, "id is required");
        this.customerId = requireNonBlank(customerId, "customerId is required");
        this.tokenHash = requireNonBlank(tokenHash, "tokenHash is required");
        this.issuedAt = issuedAt;
        this.revokedAt = revokedAt;
    }

    public static CustomerAuthSessionEntity create(
        final String id,
        final String customerId,
        final String tokenHash,
        final Instant issuedAt,
        final Instant revokedAt
    ) {
        return new CustomerAuthSessionEntity(id, customerId, tokenHash, issuedAt, revokedAt);
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

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    private static String requireNonBlank(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}
