package com.kartoush.auth.service.impl;

import com.kartoush.auth.service.PasswordSetupTokenHasher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class DefaultPasswordSetupTokenHasher implements PasswordSetupTokenHasher {

    private static final String HASH_ALGORITHM = "SHA-256";

    @Override
    public String hash(final String rawToken) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            final byte[] hashedBytes = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashedBytes);
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash credential setup token", exception);
        }
    }
}
