package com.kartoush.customer.service.job;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@ConfigurationProperties(prefix = "kartoush.jobs.activation-email")
public class ActivationEmailJobProperties {

    private String encryptionKey = "";

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(final String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    @PostConstruct
    void validate() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            throw new IllegalStateException("kartoush.jobs.activation-email.encryption-key must be configured");
        }

        final byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(encryptionKey);
        } catch (final IllegalArgumentException exception) {
            throw new IllegalStateException(
                "kartoush.jobs.activation-email.encryption-key must be valid Base64",
                exception
            );
        }

        if (decodedKey.length != 16 && decodedKey.length != 24 && decodedKey.length != 32) {
            throw new IllegalStateException(
                "kartoush.jobs.activation-email.encryption-key must decode to 16, 24, or 32 bytes"
            );
        }
    }
}
