package com.kartoush.customer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.kartoush.customer.service.ActivationTokenGenerator;
import org.junit.jupiter.api.Test;

class DefaultActivationTokenGeneratorTest {

    private static final String URL_SAFE_BASE64_PATTERN = "^[A-Za-z0-9_-]+$";

    private final ActivationTokenGenerator activationTokenGenerator =
        new DefaultActivationTokenGenerator();

    @Test
    void shouldGenerateNonBlankToken() {
        // given/when
        String token = activationTokenGenerator.generate();

        // then
        assertThat(token).isNotBlank();
    }

    @Test
    void shouldGenerateDifferentTokensAcrossCalls() {
        // given/when
        String firstToken = activationTokenGenerator.generate();
        String secondToken = activationTokenGenerator.generate();

        // then
        assertThat(firstToken).isNotEqualTo(secondToken);
    }

    @Test
    void shouldGenerateUrlSafeTokenWithoutPadding() {
        // given/when
        String token = activationTokenGenerator.generate();

        // then
        assertThat(token)
            .matches(URL_SAFE_BASE64_PATTERN)
            .doesNotContain("=");
    }

    @Test
    void shouldGenerateExpectedTokenLength() {
        // given / when
        String token = activationTokenGenerator.generate();

        // then
        assertThat(token).hasSize(43);
    }
}
