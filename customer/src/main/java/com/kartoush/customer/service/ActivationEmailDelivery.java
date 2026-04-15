package com.kartoush.customer.service;

import com.kartoush.platform.types.Email;

public record ActivationEmailDelivery(
    Email email,
    String rawToken) {
}
