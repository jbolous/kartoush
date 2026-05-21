package com.kartoush.auth.service.impl;

import com.kartoush.auth.service.PasswordSetupTokenGenerator;
import com.kartoush.auth.service.SecureTokenGenerator;
import org.springframework.stereotype.Component;

@Component
public class DefaultPasswordSetupTokenGenerator implements PasswordSetupTokenGenerator {

    private final SecureTokenGenerator secureTokenGenerator;

    public DefaultPasswordSetupTokenGenerator(final SecureTokenGenerator secureTokenGenerator) {
        this.secureTokenGenerator = secureTokenGenerator;
    }

    @Override
    public String generate() {
        return secureTokenGenerator.generate();
    }
}
