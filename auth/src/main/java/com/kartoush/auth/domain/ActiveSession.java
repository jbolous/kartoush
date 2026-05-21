package com.kartoush.auth.domain;

public record ActiveSession(
    String sessionId,
    String customerId
) {
}
