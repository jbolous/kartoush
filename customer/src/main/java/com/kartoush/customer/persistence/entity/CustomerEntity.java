package com.kartoush.customer.persistence.entity;

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
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "customer")
public class CustomerEntity implements Persistable<String> {

    @EmbeddedId
    private CustomerIdEmbeddable id;

    @Transient
    private boolean isNew = true;

    @Embedded
    private CustomerProfileEntity profile;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomerStatus customerStatus;

    @OneToMany(
        mappedBy = "customer",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private final List<CustomerAddressEntity> addresses = new ArrayList<>();

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
        CustomerStatus customerStatus
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.profile = Objects.requireNonNull(profile, "profile is required");
        this.email = email;
        this.customerStatus = Objects.requireNonNull(customerStatus, "status is required");
    }

    public static CustomerEntity newCustomer(
        CustomerIdEmbeddable id,
        CustomerProfileEntity profile,
        String email,
        CustomerStatus status
    ) {
        return new CustomerEntity(id, profile, email, status);
    }

    @Override
    public String getId() {
        return id != null ? id.getValue() : null;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public CustomerIdEmbeddable getCustomerId() {
        return id;
    }

    public CustomerProfileEntity getProfile() {
        return profile;
    }

    public String getEmail() {
        return email;
    }

    public CustomerStatus getCustomerStatus() {
        return customerStatus;
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

    public void setCustomerStatus(CustomerStatus status) {
        this.customerStatus = status;
    }

    public void setProfile(CustomerProfileEntity profile) {
        this.profile = profile;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
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

        updatedAt = Instant.now();
    }

    public void replaceAddresses(final List<CustomerAddressEntity> newAddresses) {
        addresses.clear();

        for (final CustomerAddressEntity address : newAddresses) {
            address.setCustomer(this);
            addresses.add(address);
        }
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

    public static String normalizeEmail(String value) {
        String trimmed = requireNonBlank(value, "email");
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
