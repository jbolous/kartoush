package com.kartoush.customer.facade.model;

public record CreateCustomerCommand(
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    Boolean termsAccepted,
    String termsVersion
) {
}
