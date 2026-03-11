package com.kartoush.customer.domain;

import com.kartoush.platform.types.AddressId;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class Customer {

    private final CustomerId id;
    private CustomerProfile profile;
    private String email;
    private final String passwordHash;
    private final CustomerStatus status;

    private final List<CustomerAddress> addresses = new ArrayList<>();

    private final Instant createdAt;
    private Instant updatedAt;

    private Customer(
            CustomerId id,
            CustomerProfile profile,
            String email,
            String passwordHash,
            CustomerStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.profile = Objects.requireNonNull(profile);
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Customer createNew(
            CustomerId id,
            CustomerProfile profile,
            String email,
            String passwordHash,
            Instant now) {
        return new Customer(
                id,
                profile,
                email,
                passwordHash,
                CustomerStatus.ACTIVE,
                now,
                now);
    }

    public static Customer fromPersistence(
            CustomerId id,
            CustomerProfile profile,
            String email,
            String passwordHash,
            CustomerStatus status,
            List<CustomerAddress> addresses,
            Instant createdAt,
            Instant updatedAt) {
        Customer customer = new Customer(
                id,
                profile,
                email,
                passwordHash,
                status,
                createdAt,
                updatedAt);
        customer.addresses.addAll(addresses);
        return customer;
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
        this.profile = Objects.requireNonNull(newProfile);
        touch(now);
    }

    public void changeEmail(String newEmail, Instant now) {
        this.email = normalizeEmail(newEmail);
        touch(now);
    }

    public void addAddress(final CustomerAddress address, final Instant now) {
        Objects.requireNonNull(address, "address");

        if (address.isDefaultShipping()) {
            clearDefaultShipping(now);
        }

        if (address.isDefaultBilling()) {
            clearDefaultBilling(now);
        }

        addresses.add(address);
        touch(now);
    }

    public void setDefaultShippingAddress(final AddressId addressId, final Instant now) {
        final CustomerAddress address = findAddress(addressId);

        clearDefaultShipping(now);
        address.setDefaultShipping(true, now);
        touch(now);
    }

    public void setDefaultBillingAddress(final AddressId addressId, final Instant now) {
        final CustomerAddress address = findAddress(addressId);

        clearDefaultBilling(now);
        address.setDefaultBilling(true, now);
        touch(now);
    }

    public void removeAddress(final AddressId addressId, final Instant now) {
        final boolean removed = addresses.removeIf(address -> address.getId().equals(addressId));

        if (!removed) {
            throw new IllegalArgumentException("address not found: " + addressId.value());
        }

        touch(now);
    }

    private void clearDefaultShipping(final Instant now) {
        for (final CustomerAddress address : addresses) {
            if (address.isDefaultShipping()) {
                address.setDefaultShipping(false, now);
            }
        }
    }

    private void clearDefaultBilling(final Instant now) {
        for (final CustomerAddress address : addresses) {
            if (address.isDefaultBilling()) {
                address.setDefaultBilling(false, now);
            }
        }
    }

    private CustomerAddress findAddress(final AddressId addressId) {
        return addresses.stream()
                .filter(address -> address.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("address not found: " + addressId.value()));
    }

    private void touch(Instant now) {
        if (now.isAfter(updatedAt)) {
            updatedAt = now;
        }
    }

    private static String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
