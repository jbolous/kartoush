package com.kartoush.customer.domain;

import org.springframework.util.StringUtils;

public record CustomerProfile(
        String firstName,
        String lastName,
        String phoneNumber
) {
    public CustomerProfile {
        if (!StringUtils.hasText(firstName)) {
            throw new IllegalArgumentException("firstName must not be blank");
        }
        if (!StringUtils.hasText(lastName)) {
            throw new IllegalArgumentException("lastName must not be blank");
        }
    }

    public static CustomerProfile of(String firstName, String lastName, String phoneNumber) {
        return new CustomerProfile(firstName, lastName, phoneNumber);
    }
}
