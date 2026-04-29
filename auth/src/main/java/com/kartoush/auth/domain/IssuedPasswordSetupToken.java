package com.kartoush.auth.domain;

public record IssuedPasswordSetupToken(
    PasswordSetupToken setupToken,
    String rawToken) {
}
