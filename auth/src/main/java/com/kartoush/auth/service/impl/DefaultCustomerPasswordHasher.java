package com.kartoush.auth.service.impl;

import com.kartoush.auth.service.CustomerPasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultCustomerPasswordHasher implements CustomerPasswordHasher {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String hash(final String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
