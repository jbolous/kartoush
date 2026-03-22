package com.kartoush.customer.facade.model;

public record UpdateCustomerRequest(
        String firstName,
        String lastName,
        String phoneNumber) {
}
