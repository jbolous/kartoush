package com.kartoush.auth.service.impl;

import com.kartoush.auth.domain.IssuedPasswordResetToken;
import com.kartoush.auth.domain.PasswordResetToken;
import com.kartoush.auth.exception.PasswordResetTokenConsumedException;
import com.kartoush.auth.exception.PasswordResetTokenExpiredException;
import com.kartoush.auth.exception.PasswordResetTokenNotFoundException;
import com.kartoush.auth.persistence.entity.PasswordResetTokenEntity;
import com.kartoush.auth.persistence.repository.PasswordResetTokenRepository;
import com.kartoush.auth.service.PasswordResetTokenGenerator;
import com.kartoush.auth.service.PasswordResetTokenHasher;
import com.kartoush.auth.service.PasswordResetTokenService;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.ulid.UlidGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class DefaultPasswordResetTokenService implements PasswordResetTokenService {

    private static final Duration PASSWORD_RESET_TOKEN_TTL = Duration.ofHours(24);

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final PasswordResetTokenGenerator passwordResetTokenGenerator;

    private final PasswordResetTokenHasher passwordResetTokenHasher;

    private final UlidGenerator ulidGenerator;

    private final Clock clock;

    public DefaultPasswordResetTokenService(
        final PasswordResetTokenRepository passwordResetTokenRepository,
        final PasswordResetTokenGenerator passwordResetTokenGenerator,
        final PasswordResetTokenHasher passwordResetTokenHasher,
        final UlidGenerator ulidGenerator,
        final Clock clock
    ) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetTokenGenerator = passwordResetTokenGenerator;
        this.passwordResetTokenHasher = passwordResetTokenHasher;
        this.ulidGenerator = ulidGenerator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IssuedPasswordResetToken issuePasswordResetTokenFor(final CustomerId customerId) {
        final Instant createdAt = Instant.now(clock);
        final Instant expiresAt = createdAt.plus(PASSWORD_RESET_TOKEN_TTL);
        final String rawToken = passwordResetTokenGenerator.generate();
        final String tokenHash = passwordResetTokenHasher.hash(rawToken);

        final PasswordResetTokenEntity saved =
            passwordResetTokenRepository.save(
                PasswordResetTokenEntity.create(
                    ulidGenerator.next(),
                    customerId.value(),
                    tokenHash,
                    expiresAt,
                    null,
                    createdAt
                )
            );

        return new IssuedPasswordResetToken(toDomain(saved), rawToken);
    }

    @Override
    @Transactional(readOnly = true)
    public PasswordResetToken validate(final CustomerId customerId, final String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new PasswordResetTokenNotFoundException(customerId.value());
        }

        final PasswordResetToken resetToken =
            passwordResetTokenRepository.findByCustomerIdAndTokenHash(
                    customerId.value(),
                    passwordResetTokenHasher.hash(rawToken))
                .map(this::toDomain)
                .orElseThrow(() -> new PasswordResetTokenNotFoundException(customerId.value()));

        final Instant now = Instant.now(clock);

        if (resetToken.isConsumed()) {
            throw new PasswordResetTokenConsumedException(customerId.value());
        }

        if (resetToken.isExpired(now)) {
            throw new PasswordResetTokenExpiredException(customerId.value());
        }

        return resetToken;
    }

    @Override
    @Transactional
    public PasswordResetToken consume(final PasswordResetToken resetToken) {
        final PasswordResetToken consumed = resetToken.consume(Instant.now(clock));
        final PasswordResetTokenEntity entity = PasswordResetTokenEntity.create(
            consumed.id(),
            consumed.customerId().value(),
            consumed.tokenHash(),
            consumed.expiresAt(),
            consumed.consumedAt(),
            consumed.createdAt()
        );

        return toDomain(passwordResetTokenRepository.save(entity));
    }

    private PasswordResetToken toDomain(final PasswordResetTokenEntity entity) {
        return new PasswordResetToken(
            entity.getId(),
            CustomerId.of(entity.getCustomerId()),
            entity.getTokenHash(),
            entity.getExpiresAt(),
            entity.getConsumedAt(),
            entity.getCreatedAt()
        );
    }
}
