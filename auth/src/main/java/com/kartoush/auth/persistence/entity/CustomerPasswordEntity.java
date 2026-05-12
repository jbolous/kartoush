package com.kartoush.auth.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

@Entity
@Table(name = "customer_password")
public class CustomerPasswordEntity implements Persistable<String> {

    @Id
    @Column(name = "customer_id", nullable = false, length = 26)
    private String customerId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @jakarta.persistence.Transient
    private boolean isNew = true;

    public CustomerPasswordEntity() {
        // for JPA only
    }

    private CustomerPasswordEntity(final String customerId, final String passwordHash) {
        this.customerId = requireNonBlank(customerId, "customerId is required");
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash is required");
    }

    public static CustomerPasswordEntity create(final String customerId, final String passwordHash) {
        return new CustomerPasswordEntity(customerId, passwordHash);
    }

    @Override
    public String getId() {
        return customerId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setPasswordHash(final String passwordHash) {
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash is required");
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    void onCreate() {
        if (customerId == null) {
            throw new IllegalStateException("customerId must be set before persisting CustomerPasswordEntity");
        }

        final Instant now = Instant.now();
        passwordHash = requireNonBlank(passwordHash, "passwordHash");

        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        passwordHash = requireNonBlank(passwordHash, "passwordHash");
        updatedAt = Instant.now();
    }

    private static String requireNonBlank(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
