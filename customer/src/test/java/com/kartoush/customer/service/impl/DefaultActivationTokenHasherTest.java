package com.kartoush.customer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.kartoush.customer.service.ActivationTokenHasher;
import org.junit.jupiter.api.Test;

class DefaultActivationTokenHasherTest {

    private static final String HEX_PATTERN = "^[0-9a-f]+$";
    private static final String SAMPLE_ACTIVATION_TOKEN = "sample-activation-token";
    private static final String HASHED_SAMPLE_ACTIVATION_TOKEN = "ca288cabaeeb1144926c4d530e0cae0b5e4ae5d0b299fe104d0f385516191874";
    private static final String FIRST_TOKEN = "first-token";
    private static final String SECOND_TOKEN = "second-token";

    private final ActivationTokenHasher activationTokenHasher =
        new DefaultActivationTokenHasher();

    @Test
    void shouldReturnConsistentHashForSameToken() {
        // given
        String token = SAMPLE_ACTIVATION_TOKEN;

        // when
        String firstHash = activationTokenHasher.hash(token);
        String secondHash = activationTokenHasher.hash(token);

        // then
        assertThat(firstHash).isEqualTo(secondHash);
    }

    @Test
    void shouldReturnDifferentHashForDifferentTokens() {
        // given/when
        String firstHash = activationTokenHasher.hash(FIRST_TOKEN);
        String secondHash = activationTokenHasher.hash(SECOND_TOKEN);

        // then
        assertThat(firstHash).isNotEqualTo(secondHash);
    }

    @Test
    void shouldReturnLowercaseHexHash() {
        // given/when
        String hash = activationTokenHasher.hash(SAMPLE_ACTIVATION_TOKEN);

        // then
        assertThat(hash).matches(HEX_PATTERN);
    }

    @Test
    void shouldReturnHashWithExpectedLength() {
        // given/when
        String hash = activationTokenHasher.hash(SAMPLE_ACTIVATION_TOKEN);

        // then
        assertThat(hash).hasSize(64);
    }

    @Test
    void shouldReturnKnownHashForToken() {
        // given/when
        String hash = activationTokenHasher.hash(SAMPLE_ACTIVATION_TOKEN);

        // then
        assertThat(hash)
            .isEqualTo(HASHED_SAMPLE_ACTIVATION_TOKEN);
    }
}
