package com.kartoush.auth.domain;

public record IssuedCustomerAccessToken(
    String accessToken,
    String tokenType
) {
}
