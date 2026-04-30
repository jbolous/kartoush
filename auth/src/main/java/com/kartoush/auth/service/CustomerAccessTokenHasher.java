package com.kartoush.auth.service;

public interface CustomerAccessTokenHasher {

    String hash(String rawToken);
}
