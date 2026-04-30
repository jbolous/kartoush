package com.kartoush.auth.service.impl;

import com.kartoush.auth.service.PasswordResetTokenGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class DefaultPasswordResetTokenGenerator implements PasswordResetTokenGenerator {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        final byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(tokenBytes);
    }
}
