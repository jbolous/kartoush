package com.kartoush.customer.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import org.springframework.util.StringUtils;

public final class Customer {

    private final CustomerId id;
    private CustomerProfile profile;
    private String email;
    private final String passwordHash;
    private final CustomerStatus status;
    private final List<CustomerAddress> addresses;

    private final Instant createdAt;
    private Instant updatedAt;

    public Customer(
            CustomerId id,
            CustomerProfile profile,
            String email,
            String passwordHash,
            CustomerStatus status,
            List<CustomerAddress> addresses,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.email = normalizeEmail(email);
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash");
        this.status = Objects.requireNonNull(status, "status");
        this.addresses = new ArrayList<>(Objects.requireNonNull(addresses, "addresses"));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Customer fromPersistence(
            CustomerId id,
            CustomerProfile profile,
            String email,
            String passwordHash,
            CustomerStatus status,
            List<CustomerAddress> addresses,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Customer(id, profile, email, passwordHash, status, addresses, createdAt, updatedAt);
    }

    public static Customer createNew(
            CustomerId id,
            CustomerProfile profile,
            String email,
            String passwordHash,
            Instant now) {
        Objects.requireNonNull(now, "now");

        return new Customer(
                id,
                profile,
                email,
                passwordHash,
                CustomerStatus.ACTIVE,
                List.of(),
                now,
                now
        );
    }

    public CustomerId getId() {
        return id;
    }

    public CustomerProfile getProfile() {
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

    public List<CustomerAddress> getAddresses() {
        return List.copyOf(addresses);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateProfile(CustomerProfile newProfile, Instant now) {
        this.profile = Objects.requireNonNull(newProfile, "newProfile");
        touch(now);
    }

    public void changeEmail(String newEmail, Instant now) {
        this.email = normalizeEmail(newEmail);
        touch(now);
    }

    public boolean matchesPasswordHash(String candidatePasswordHash) {
        return StringUtils.hasText(candidatePasswordHash) && candidatePasswordHash.equals(this.passwordHash);
    }

    private void touch(Instant now) {
        Objects.requireNonNull(now, "now");
        if (now.isAfter(updatedAt)) {
            updatedAt = now;
        }
    }

    private static String normalizeEmail(String value) {
        String trimmed = requireNonBlank(value, "email");
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
