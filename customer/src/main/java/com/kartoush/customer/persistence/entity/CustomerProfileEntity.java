package com.kartoush.customer.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Embeddable
public class CustomerProfileEntity {

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    protected CustomerProfileEntity() {
        // JPA
    }

    public CustomerProfileEntity(String firstName, String lastName, String phoneNumber) {
        this.firstName = requireNonBlank(firstName, "firstName");
        this.lastName = requireNonBlank(lastName, "lastName");
        this.phoneNumber = normalizeOptional(phoneNumber);
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    protected void normalize() {
        firstName = requireNonBlank(firstName, "firstName");
        lastName = requireNonBlank(lastName, "lastName");
        phoneNumber = normalizeOptional(phoneNumber);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CustomerProfileEntity that = (CustomerProfileEntity) o;
        return Objects.equals(firstName, that.firstName) &&
            Objects.equals(lastName, that.lastName) &&
            Objects.equals(phoneNumber, that.phoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, phoneNumber);
    }
}
