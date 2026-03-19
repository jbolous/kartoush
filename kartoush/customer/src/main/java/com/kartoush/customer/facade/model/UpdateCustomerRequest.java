package com.kartoush.customer.facade.model;

public record UpdateCustomerRequest(
        String email,
        String phoneNumber,
        String firstName,
        String lastName) {
}
