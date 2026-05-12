package com.kartoush.customer.domain;

import com.kartoush.platform.types.AddressId;

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

    private boolean defaultShipping;

    private boolean defaultBilling;

    public CustomerAddress(
        final AddressId id,
        final String label,
        final String line1,
        final String line2,
        final String city,
        final String stateOrProvince,
        final String postalCode,
        final String countryCode,
        final boolean defaultShipping,
        final boolean defaultBilling
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = normalizeOptional(label);
        this.line1 = requireNonBlank(line1, "line1");
        this.line2 = normalizeOptional(line2);
        this.city = requireNonBlank(city, "city");
        this.stateOrProvince = requireNonBlank(stateOrProvince, "stateOrProvince");
        this.postalCode = requireNonBlank(postalCode, "postalCode");
        this.countryCode = requireNonBlank(countryCode, "countryCode");
        this.defaultShipping = defaultShipping;
        this.defaultBilling = defaultBilling;
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
        final boolean defaultShipping,
        final boolean defaultBilling
    ) {
        return new CustomerAddress(
            id,
            label,
            line1,
            line2,
            city,
            stateOrProvince,
            postalCode,
            countryCode,
            defaultShipping,
            defaultBilling
        );
    }

    public static CustomerAddress fromPersistence(
        final AddressId id,
        final String label,
        final String line1,
        final String line2,
        final String city,
        final String stateOrProvince,
        final String postalCode,
        final String countryCode,
        final boolean defaultShipping,
        final boolean defaultBilling
    ) {
        return new CustomerAddress(
            id,
            label,
            line1,
            line2,
            city,
            stateOrProvince,
            postalCode,
            countryCode,
            defaultShipping,
            defaultBilling
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

    public boolean isDefaultShipping() {
        return defaultShipping;
    }

    public boolean isDefaultBilling() {
        return defaultBilling;
    }

    public void setDefaultShipping(final boolean value) {
        this.defaultShipping = value;
    }

    public void setDefaultBilling(final boolean value) {
        this.defaultBilling = value;
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
