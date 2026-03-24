package com.kartoush.customer.facade.model;

public record CreateCustomerRequest(
        String firstName,
        String lastName,
        String email,
        String phoneNumber) {
}
