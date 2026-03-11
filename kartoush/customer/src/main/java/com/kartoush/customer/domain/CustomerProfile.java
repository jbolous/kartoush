package com.kartoush.customer.domain;

public record CustomerProfile(
        String firstName,
        String lastName,
        String phoneNumber
) {
    public CustomerProfile {
        if (firstName == null || firstName.isBlank()){
            throw new IllegalArgumentException("firstName must not be blank");
        }
        if (lastName == null || lastName.isBlank()){
            throw new IllegalArgumentException("lastName must not be blank");
        }
    }

    public static CustomerProfile of(String firstName, String lastName, String phoneNumber) {
        return new CustomerProfile(firstName, lastName, phoneNumber);
    }
}
