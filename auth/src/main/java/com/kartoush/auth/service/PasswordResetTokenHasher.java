package com.kartoush.auth.service;

public interface PasswordResetTokenHasher {

    String hash(String rawToken);
}
