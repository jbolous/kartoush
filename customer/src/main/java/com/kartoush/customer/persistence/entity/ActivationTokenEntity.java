package com.kartoush.customer.persistence.entity;

import com.kartoush.customer.persistence.model.ActivationTokenIdEmbeddable;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "activation_token")
public class ActivationTokenEntity {

    @EmbeddedId
    private ActivationTokenIdEmbeddable id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "customer_id", nullable = false))
    private CustomerIdEmbeddable customerId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ActivationTokenEntity() {
    }

    protected ActivationTokenEntity(
        final ActivationTokenIdEmbeddable id,
        final CustomerIdEmbeddable customerId,
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

    public static ActivationTokenEntity of(
        final ActivationTokenIdEmbeddable id,
        final CustomerIdEmbeddable customerId,
        final String tokenHash,
        final Instant expiresAt,
        final Instant consumedAt,
        final Instant createdAt) {
        return new ActivationTokenEntity(
            id,
            customerId,
            tokenHash,
            expiresAt,
            consumedAt,
            createdAt);
    }

    public ActivationTokenIdEmbeddable getId() {
        return id;
    }

    public void setId(ActivationTokenIdEmbeddable id) {
        this.id = id;
    }

    public CustomerIdEmbeddable getCustomerId() {
        return customerId;
    }

    public void setCustomerId(CustomerIdEmbeddable customerId) {
        this.customerId = customerId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(Instant consumedAt) {
        this.consumedAt = consumedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
