package com.kartoush.config.security;

public record AuthenticatedPrincipal(
    String customerId,
    String email
) {
}
