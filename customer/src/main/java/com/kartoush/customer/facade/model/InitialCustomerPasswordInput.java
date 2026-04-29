package com.kartoush.customer.facade.model;

public record InitialCustomerPasswordInput(
    String setupToken,
    String password,
    String confirmPassword) {
}
