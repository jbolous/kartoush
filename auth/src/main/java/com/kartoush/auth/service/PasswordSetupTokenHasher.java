package com.kartoush.auth.service;

public interface PasswordSetupTokenHasher {

    String hash(String rawToken);
}
