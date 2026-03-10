package com.kartoush.customer.persistence.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.platform.types.CustomerStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class CustomerEntity {

    @EmbeddedId
    private CustomerIdEmbeddable id;

    @Embedded
    private CustomerProfileEntity profile;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomerStatus status;

    @OneToMany(
            mappedBy = "customer",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<CustomerAddressEntity> addresses = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CustomerEntity() {
        // for JPA only
    }

    private CustomerEntity(
            CustomerIdEmbeddable id,
            CustomerProfileEntity profile,
            String email,
            String passwordHash,
            CustomerStatus status
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.profile = Objects.requireNonNull(profile, "profile is required");
        this.email = normalizeEmail(email);
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash is required");
        this.status = Objects.requireNonNull(status, "status is required");
    }

    public static CustomerEntity newCustomer(
            CustomerIdEmbeddable id,
            CustomerProfileEntity profile,
            String email,
            String passwordHash,
            CustomerStatus status
    ) {
        return new CustomerEntity(id, profile, email, passwordHash, status);
    }

    public CustomerIdEmbeddable getId() {
        return id;
    }

    public CustomerProfileEntity getProfile() {
        return profile;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public List<CustomerAddressEntity> getAddresses() {
        return addresses;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(CustomerIdEmbeddable id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    public void setAddresses(List<CustomerAddressEntity> addresses) {
        this.addresses = addresses;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setProfile(CustomerProfileEntity profile) {
        this.profile = profile;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void replaceAddresses(List<CustomerAddressEntity> newAddresses) {
        addresses.clear();
        addresses.addAll(newAddresses);
    }

    @PrePersist
    public void onCreate() {
        if (id == null) {
            throw new IllegalStateException("id must be set before persisting CustomerEntity");
        }
        if (profile == null) {
            throw new IllegalStateException("profile must be set before persisting CustomerEntity");
        }

        Instant now = Instant.now();

        profile.normalize();
        email = normalizeEmail(email);
        passwordHash = requireNonBlank(passwordHash, "passwordHash");

        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        if (profile == null) {
            throw new IllegalStateException("profile must be set before updating CustomerEntity");
        }

        profile.normalize();
        email = normalizeEmail(email);
        passwordHash = requireNonBlank(passwordHash, "passwordHash");

        updatedAt = Instant.now();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeEmail(String value) {
        String trimmed = requireNonBlank(value, "email");
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
