package com.kartoush.auth.service;

public interface CustomerPasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
