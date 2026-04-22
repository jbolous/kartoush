package com.kartoush.customer.facade.model;

public record UpdateCustomerCommand(
    String firstName,
    String lastName,
    String phoneNumber
) {
}
