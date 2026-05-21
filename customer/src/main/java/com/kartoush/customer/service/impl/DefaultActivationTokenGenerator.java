package com.kartoush.customer.service.impl;

import com.kartoush.auth.service.SecureTokenGenerator;
import com.kartoush.customer.service.ActivationTokenGenerator;
import org.springframework.stereotype.Component;

@Component
public class DefaultActivationTokenGenerator implements ActivationTokenGenerator {

    private final SecureTokenGenerator secureTokenGenerator;

    public DefaultActivationTokenGenerator(final SecureTokenGenerator secureTokenGenerator) {
        this.secureTokenGenerator = secureTokenGenerator;
    }

    @Override
    public String generate() {
        return secureTokenGenerator.generate();
    }
}
