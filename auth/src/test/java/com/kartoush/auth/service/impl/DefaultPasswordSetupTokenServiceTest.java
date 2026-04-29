package com.kartoush.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kartoush.auth.domain.PasswordSetupToken;
import com.kartoush.auth.domain.IssuedPasswordSetupToken;
import com.kartoush.auth.exception.PasswordSetupTokenConsumedException;
import com.kartoush.auth.exception.PasswordSetupTokenExpiredException;
import com.kartoush.auth.exception.PasswordSetupTokenNotFoundException;
import com.kartoush.auth.persistence.entity.PasswordSetupTokenEntity;
import com.kartoush.auth.persistence.repository.PasswordSetupTokenRepository;
import com.kartoush.auth.service.PasswordSetupTokenGenerator;
import com.kartoush.auth.service.PasswordSetupTokenHasher;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.ulid.UlidGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPasswordSetupTokenServiceTest {

    private static final CustomerId CUSTOMER_ID =
        CustomerId.of("01J2Z5Y6K4Z6D5H2X3JH8M9N0P");
    private static final String SETUP_TOKEN_ID = "01JSETUPTOKENID0000000000000";
    private static final String RAW_SETUP_TOKEN = "setup-token";
    private static final String SETUP_TOKEN_HASH = "setup-token-hash";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-27T15:00:00Z");

    @Mock
    private PasswordSetupTokenRepository passwordSetupTokenRepository;

    @Mock
    private PasswordSetupTokenGenerator passwordSetupTokenGenerator;

    @Mock
    private PasswordSetupTokenHasher passwordSetupTokenHasher;

    @Mock
    private UlidGenerator ulidGenerator;

    @Mock
    private Clock clock;

    @InjectMocks
    private DefaultPasswordSetupTokenService passwordSetupTokenService;

    @Test
    void shouldIssueSetupToken() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(passwordSetupTokenGenerator.generate()).thenReturn(RAW_SETUP_TOKEN);
        when(passwordSetupTokenHasher.hash(RAW_SETUP_TOKEN)).thenReturn(SETUP_TOKEN_HASH);
        when(ulidGenerator.next()).thenReturn(SETUP_TOKEN_ID);
        when(passwordSetupTokenRepository.save(any(PasswordSetupTokenEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        final IssuedPasswordSetupToken issued =
            passwordSetupTokenService.issueFor(CUSTOMER_ID);

        assertThat(issued.rawToken()).isEqualTo(RAW_SETUP_TOKEN);
        assertThat(issued.setupToken().customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(issued.setupToken().tokenHash()).isEqualTo(SETUP_TOKEN_HASH);
        verify(passwordSetupTokenRepository).save(any(PasswordSetupTokenEntity.class));
    }

    @Test
    void shouldValidateActiveSetupToken() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(passwordSetupTokenHasher.hash(RAW_SETUP_TOKEN)).thenReturn(SETUP_TOKEN_HASH);
        when(passwordSetupTokenRepository.findByCustomerIdAndTokenHash(CUSTOMER_ID.value(), SETUP_TOKEN_HASH))
            .thenReturn(Optional.of(activeTokenEntity()));

        final PasswordSetupToken token =
            passwordSetupTokenService.validate(CUSTOMER_ID, RAW_SETUP_TOKEN);

        assertThat(token.customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void shouldRejectMissingSetupToken() {
        assertThatThrownBy(() -> passwordSetupTokenService.validate(CUSTOMER_ID, " "))
            .isInstanceOf(PasswordSetupTokenNotFoundException.class);
    }

    @Test
    void shouldRejectExpiredSetupToken() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(passwordSetupTokenHasher.hash(RAW_SETUP_TOKEN)).thenReturn(SETUP_TOKEN_HASH);
        when(passwordSetupTokenRepository.findByCustomerIdAndTokenHash(CUSTOMER_ID.value(), SETUP_TOKEN_HASH))
            .thenReturn(Optional.of(
                PasswordSetupTokenEntity.create(
                    SETUP_TOKEN_ID,
                    CUSTOMER_ID.value(),
                    SETUP_TOKEN_HASH,
                    FIXED_INSTANT.minusSeconds(1),
                    null,
                    FIXED_INSTANT.minusSeconds(60)
                )
            ));

        assertThatThrownBy(() -> passwordSetupTokenService.validate(CUSTOMER_ID, RAW_SETUP_TOKEN))
            .isInstanceOf(PasswordSetupTokenExpiredException.class);
    }

    @Test
    void shouldRejectConsumedSetupToken() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(passwordSetupTokenHasher.hash(RAW_SETUP_TOKEN)).thenReturn(SETUP_TOKEN_HASH);
        when(passwordSetupTokenRepository.findByCustomerIdAndTokenHash(CUSTOMER_ID.value(), SETUP_TOKEN_HASH))
            .thenReturn(Optional.of(
                PasswordSetupTokenEntity.create(
                    SETUP_TOKEN_ID,
                    CUSTOMER_ID.value(),
                    SETUP_TOKEN_HASH,
                    FIXED_INSTANT.plusSeconds(60),
                    FIXED_INSTANT.minusSeconds(1),
                    FIXED_INSTANT.minusSeconds(120)
                )
            ));

        assertThatThrownBy(() -> passwordSetupTokenService.validate(CUSTOMER_ID, RAW_SETUP_TOKEN))
            .isInstanceOf(PasswordSetupTokenConsumedException.class);
    }

    @Test
    void shouldConsumeSetupToken() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(passwordSetupTokenRepository.save(any(PasswordSetupTokenEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        final PasswordSetupToken consumed =
            passwordSetupTokenService.consume(
                new PasswordSetupToken(
                    SETUP_TOKEN_ID,
                    CUSTOMER_ID,
                    SETUP_TOKEN_HASH,
                    FIXED_INSTANT.plusSeconds(60),
                    null,
                    FIXED_INSTANT.minusSeconds(60)
                )
            );

        assertThat(consumed.consumedAt()).isEqualTo(FIXED_INSTANT);
    }

    private PasswordSetupTokenEntity activeTokenEntity() {
        return PasswordSetupTokenEntity.create(
            SETUP_TOKEN_ID,
            CUSTOMER_ID.value(),
            SETUP_TOKEN_HASH,
            FIXED_INSTANT.plusSeconds(60),
            null,
            FIXED_INSTANT.minusSeconds(60)
        );
    }
}
