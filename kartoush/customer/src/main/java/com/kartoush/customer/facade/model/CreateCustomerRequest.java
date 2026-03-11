package com.kartoush.customer.facade.model;

public record CreateCustomerRequest(
        String email,
        String password,
        String phoneNumber,
        String firstName,
        String lastName) {
}
