package com.kartoush.auth.service.impl;

import com.kartoush.auth.domain.IssuedPasswordSetupToken;
import com.kartoush.auth.domain.PasswordSetupToken;
import com.kartoush.auth.exception.PasswordSetupTokenConsumedException;
import com.kartoush.auth.exception.PasswordSetupTokenExpiredException;
import com.kartoush.auth.exception.PasswordSetupTokenNotFoundException;
import com.kartoush.auth.persistence.entity.PasswordSetupTokenEntity;
import com.kartoush.auth.persistence.repository.PasswordSetupTokenRepository;
import com.kartoush.auth.service.PasswordSetupTokenGenerator;
import com.kartoush.auth.service.PasswordSetupTokenHasher;
import com.kartoush.auth.service.PasswordSetupTokenService;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.ulid.UlidGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class DefaultPasswordSetupTokenService implements PasswordSetupTokenService {

    private static final Duration PASSWORD_SETUP_TOKEN_TTL = Duration.ofHours(24);

    private final PasswordSetupTokenRepository passwordSetupTokenRepository;

    private final PasswordSetupTokenGenerator passwordSetupTokenGenerator;

    private final PasswordSetupTokenHasher passwordSetupTokenHasher;

    private final UlidGenerator ulidGenerator;

    private final Clock clock;

    public DefaultPasswordSetupTokenService(
        final PasswordSetupTokenRepository passwordSetupTokenRepository,
        final PasswordSetupTokenGenerator passwordSetupTokenGenerator,
        final PasswordSetupTokenHasher passwordSetupTokenHasher,
        final UlidGenerator ulidGenerator,
        final Clock clock) {
        this.passwordSetupTokenRepository = passwordSetupTokenRepository;
        this.passwordSetupTokenGenerator = passwordSetupTokenGenerator;
        this.passwordSetupTokenHasher = passwordSetupTokenHasher;
        this.ulidGenerator = ulidGenerator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IssuedPasswordSetupToken issueFor(final CustomerId customerId) {
        final Instant createdAt = Instant.now(clock);
        final Instant expiresAt = createdAt.plus(PASSWORD_SETUP_TOKEN_TTL);
        final String rawToken = passwordSetupTokenGenerator.generate();
        final String tokenHash = passwordSetupTokenHasher.hash(rawToken);

        final PasswordSetupTokenEntity saved =
            passwordSetupTokenRepository.save(
                PasswordSetupTokenEntity.create(
                    ulidGenerator.next(),
                    customerId.value(),
                    tokenHash,
                    expiresAt,
                    null,
                    createdAt
                )
            );

        return new IssuedPasswordSetupToken(toDomain(saved), rawToken);
    }

    @Override
    @Transactional(readOnly = true)
    public PasswordSetupToken validate(final CustomerId customerId, final String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new PasswordSetupTokenNotFoundException(customerId.value());
        }

        final PasswordSetupToken setupToken =
            passwordSetupTokenRepository.findByCustomerIdAndTokenHash(
                    customerId.value(),
                    passwordSetupTokenHasher.hash(rawToken))
                .map(this::toDomain)
                .orElseThrow(() -> new PasswordSetupTokenNotFoundException(customerId.value()));

        final Instant now = Instant.now(clock);

        if (setupToken.isConsumed()) {
            throw new PasswordSetupTokenConsumedException(customerId.value());
        }

        if (setupToken.isExpired(now)) {
            throw new PasswordSetupTokenExpiredException(customerId.value());
        }

        return setupToken;
    }

    @Override
    @Transactional
    public PasswordSetupToken consume(final PasswordSetupToken setupToken) {
        final PasswordSetupToken consumed = setupToken.consume(Instant.now(clock));
        final PasswordSetupTokenEntity entity = PasswordSetupTokenEntity.create(
            consumed.id(),
            consumed.customerId().value(),
            consumed.tokenHash(),
            consumed.expiresAt(),
            consumed.consumedAt(),
            consumed.createdAt());

        return toDomain(passwordSetupTokenRepository.save(entity));
    }

    private PasswordSetupToken toDomain(final PasswordSetupTokenEntity entity) {
        return new PasswordSetupToken(
            entity.getId(),
            CustomerId.of(entity.getCustomerId()),
            entity.getTokenHash(),
            entity.getExpiresAt(),
            entity.getConsumedAt(),
            entity.getCreatedAt()
        );
    }
}
