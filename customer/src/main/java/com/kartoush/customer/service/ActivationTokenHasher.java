package com.kartoush.customer.service;

public interface ActivationTokenHasher {

    String hash(String rawToken);
}
