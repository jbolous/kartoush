package com.kartoush.customer.domain;

import com.kartoush.platform.types.AddressId;
import com.kartoush.platform.types.AddressType;

import java.time.Instant;
import java.util.Objects;

public final class CustomerAddress {

    private final AddressId id;
    private final String label;
    private final String line1;
    private final String line2;
    private final String city;
    private final String stateOrProvince;
    private final String postalCode;
    private final String countryCode;
    private final AddressType type;

    private boolean defaultShipping;
    private boolean defaultBilling;

    private final Instant createdAt;
    private Instant updatedAt;

    public CustomerAddress(
            final AddressId id,
            final String label,
            final String line1,
            final String line2,
            final String city,
            final String stateOrProvince,
            final String postalCode,
            final String countryCode,
            final AddressType type,
            final boolean defaultShipping,
            final boolean defaultBilling,
            final Instant createdAt,
            final Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = normalizeOptional(label);
        this.line1 = requireNonBlank(line1, "line1");
        this.line2 = normalizeOptional(line2);
        this.city = requireNonBlank(city, "city");
        this.stateOrProvince = requireNonBlank(stateOrProvince, "stateOrProvince");
        this.postalCode = requireNonBlank(postalCode, "postalCode");
        this.countryCode = requireNonBlank(countryCode, "countryCode");
        this.type = Objects.requireNonNull(type, "type");
        this.defaultShipping = defaultShipping;
        this.defaultBilling = defaultBilling;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static CustomerAddress createNew(
            final AddressId id,
            final String label,
            final String line1,
            final String line2,
            final String city,
            final String stateOrProvince,
            final String postalCode,
            final String countryCode,
            final AddressType type,
            final boolean defaultShipping,
            final boolean defaultBilling,
            final Instant now
    ) {
        Objects.requireNonNull(now, "now");

        return new CustomerAddress(
                id,
                label,
                line1,
                line2,
                city,
                stateOrProvince,
                postalCode,
                countryCode,
                type,
                defaultShipping,
                defaultBilling,
                now,
                now
        );
    }

    public AddressId getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getCity() {
        return city;
    }

    public String getStateOrProvince() {
        return stateOrProvince;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public AddressType getType() {
        return type;
    }

    public boolean isDefaultShipping() {
        return defaultShipping;
    }

    public boolean isDefaultBilling() {
        return defaultBilling;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setDefaultShipping(final boolean value, final Instant now) {
        this.defaultShipping = value;
        touch(now);
    }

    public void setDefaultBilling(final boolean value, final Instant now) {
        this.defaultBilling = value;
        touch(now);
    }

    private void touch(final Instant now) {
        Objects.requireNonNull(now, "now");

        if (now.isAfter(updatedAt)) {
            updatedAt = now;
        }
    }

    private static String requireNonBlank(final String value, final String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value.trim();
    }

    private static String normalizeOptional(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
