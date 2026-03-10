package com.kartoush.customer.domain;

import com.kartoush.platform.types.AddressId;
import com.kartoush.platform.types.AddressType;
import org.springframework.util.StringUtils;

public record CustomerAddress(
        AddressId id,
        Customer customer,
        String label,
        String line1,
        String line2,
        String city,
        String stateOrProvince,
        String postalCode,
        String countryCode,      // ISO-3166-1 alpha-2 recommended, but Phase 1 keeps it simple
        AddressType type,
        boolean defaultShipping,
        boolean defaultBilling
) {
    public CustomerAddress {
        if (id != null && !StringUtils.hasText(id.value())){
            throw new IllegalArgumentException("id must not be blank");
        }
        if (customer != null && customer.getId() != null && !StringUtils.hasText(customer.getId().value())) {
            throw new IllegalArgumentException("customer must not be null");
        }
        if (!StringUtils.hasText(line1)) {
            throw new IllegalArgumentException("line1 must not be blank");
        }
        if (!StringUtils.hasText(city)) {
            throw new IllegalArgumentException("city must not be blank");
        }
        if (!StringUtils.hasText(stateOrProvince)) {
            throw new IllegalArgumentException("stateOrProvince must not be blank");
        }
        if (!StringUtils.hasText(postalCode)) {
            throw new IllegalArgumentException("postalCode must not be blank");
        }
        if (!StringUtils.hasText(countryCode)) {
            throw new IllegalArgumentException("countryCode must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
    }

    public CustomerAddress withDefaultShipping(boolean value) {
        return new CustomerAddress(
                id, customer, label, line1, line2, city, stateOrProvince, postalCode, countryCode, type,
                value, defaultBilling
        );
    }

    public CustomerAddress withDefaultBilling(boolean value) {
        return new CustomerAddress(
                id, customer, label, line1, line2, city, stateOrProvince, postalCode, countryCode, type,
                defaultShipping, value
        );
    }
}
