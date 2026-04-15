package com.kartoush.customer.service;

import com.kartoush.customer.domain.ActivationToken;

public record IssuedActivationToken(
    ActivationToken activationToken,
    String rawToken) {
}
