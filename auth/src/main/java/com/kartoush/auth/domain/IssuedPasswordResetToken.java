package com.kartoush.auth.domain;

public record IssuedPasswordResetToken(
    PasswordResetToken resetToken,
    String rawToken
) {
}
