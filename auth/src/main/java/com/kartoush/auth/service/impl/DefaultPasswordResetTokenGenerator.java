package com.kartoush.auth.service.impl;

import com.kartoush.auth.service.PasswordResetTokenGenerator;
import com.kartoush.auth.service.SecureTokenGenerator;
import org.springframework.stereotype.Component;

@Component
public class DefaultPasswordResetTokenGenerator implements PasswordResetTokenGenerator {

    private final SecureTokenGenerator secureTokenGenerator;

    public DefaultPasswordResetTokenGenerator(final SecureTokenGenerator secureTokenGenerator) {
        this.secureTokenGenerator = secureTokenGenerator;
    }

    @Override
    public String generate() {
        return secureTokenGenerator.generate();
    }
}
