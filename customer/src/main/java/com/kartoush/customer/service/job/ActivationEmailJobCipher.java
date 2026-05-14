package com.kartoush.customer.service.job;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class ActivationEmailJobCipher {

    private static final String CURRENT_PAYLOAD_VERSION = "v1";

    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";

    private static final int IV_LENGTH_BYTES = 12;

    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKeySpec;

    private final SecureRandom secureRandom = new SecureRandom();

    public ActivationEmailJobCipher(final ActivationEmailJobProperties activationEmailJobProperties) {
        this.secretKeySpec = new SecretKeySpec(
            Base64.getDecoder().decode(activationEmailJobProperties.getEncryptionKey()),
            "AES");
    }

    public String encrypt(final String rawToken) {
        return CURRENT_PAYLOAD_VERSION + ":" + encryptV1(rawToken);
    }

    public String decrypt(final String encryptedRawToken) {
        final int versionSeparatorIndex = encryptedRawToken.indexOf(':');

        if (versionSeparatorIndex <= 0 || versionSeparatorIndex == encryptedRawToken.length() - 1) {
            throw new IllegalStateException("Encrypted activation email job token payload version is invalid");
        }

        final String payloadVersion = encryptedRawToken.substring(0, versionSeparatorIndex);
        final String encodedPayload = encryptedRawToken.substring(versionSeparatorIndex + 1);

        if (CURRENT_PAYLOAD_VERSION.equals(payloadVersion)) {
            return decryptV1(encodedPayload);
        }

        throw new IllegalStateException(
            "Unsupported activation email job token payload version: " + payloadVersion
        );
    }

    private String encryptV1(final String rawToken) {
        try {
            final byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            final byte[] encryptedBytes = cipher.doFinal(rawToken.getBytes(StandardCharsets.UTF_8));

            final byte[] payload = Arrays.copyOf(iv, iv.length + encryptedBytes.length);
            System.arraycopy(encryptedBytes, 0, payload, iv.length, encryptedBytes.length);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (final GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt activation email job token", exception);
        }
    }

    private String decryptV1(final String encodedPayload) {
        try {
            final byte[] payload = Base64.getUrlDecoder().decode(encodedPayload);
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new IllegalStateException("Encrypted activation email job token payload is invalid");
            }

            final byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
            final byte[] encryptedBytes = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);

            final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            final byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (final GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt activation email job token", exception);
        }
    }
}
