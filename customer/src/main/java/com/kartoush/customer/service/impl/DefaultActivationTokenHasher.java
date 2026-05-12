package com.kartoush.customer.service.impl;

import com.kartoush.customer.service.ActivationTokenHasher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class DefaultActivationTokenHasher implements ActivationTokenHasher {

    private static final String HASH_ALGORITHM = "SHA-256";

    @Override
    public String hash(String rawToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashedBytes = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash activation token", exception);
        }
    }
}
