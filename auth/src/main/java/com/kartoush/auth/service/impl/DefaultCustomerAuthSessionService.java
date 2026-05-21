package com.kartoush.auth.service.impl;

import com.kartoush.auth.domain.ActiveSession;
import com.kartoush.auth.domain.IssuedCustomerAccessToken;
import com.kartoush.auth.persistence.entity.CustomerAuthSessionEntity;
import com.kartoush.auth.persistence.repository.CustomerAuthSessionRepository;
import com.kartoush.auth.service.SecureTokenGenerator;
import com.kartoush.auth.service.CustomerAccessTokenHasher;
import com.kartoush.auth.service.CustomerAuthSessionService;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.ulid.UlidGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class DefaultCustomerAuthSessionService implements CustomerAuthSessionService {

    private static final String TOKEN_TYPE = "Bearer";

    private final CustomerAuthSessionRepository customerAuthSessionRepository;

    private final SecureTokenGenerator secureTokenGenerator;

    private final CustomerAccessTokenHasher customerAccessTokenHasher;

    private final UlidGenerator ulidGenerator;

    private final Clock clock;

    public DefaultCustomerAuthSessionService(
        final CustomerAuthSessionRepository customerAuthSessionRepository,
        final SecureTokenGenerator secureTokenGenerator,
        final CustomerAccessTokenHasher customerAccessTokenHasher,
        final UlidGenerator ulidGenerator,
        final Clock clock
    ) {
        this.customerAuthSessionRepository = customerAuthSessionRepository;
        this.secureTokenGenerator = secureTokenGenerator;
        this.customerAccessTokenHasher = customerAccessTokenHasher;
        this.ulidGenerator = ulidGenerator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IssuedCustomerAccessToken issueFor(final CustomerId customerId) {
        final Instant issuedAt = Instant.now(clock);
        final String rawToken = secureTokenGenerator.generate();
        final String tokenHash = customerAccessTokenHasher.hash(rawToken);

        customerAuthSessionRepository.save(
            CustomerAuthSessionEntity.create(
                ulidGenerator.next(),
                customerId.value(),
                tokenHash,
                issuedAt,
                null
            )
        );

        return new IssuedCustomerAccessToken(rawToken, TOKEN_TYPE);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ActiveSession> findActiveCustomerByAccessToken(final String accessToken) {
        final String tokenHash = customerAccessTokenHasher.hash(accessToken);

        return customerAuthSessionRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
            .map(session -> new ActiveSession(session.getId(), session.getCustomerId()));
    }

    @Override
    @Transactional
    public void revokeAllFor(final CustomerId customerId) {
        final Instant revokedAt = Instant.now(clock);

        customerAuthSessionRepository.findAllActiveSessionsByCustomerId(customerId.value())
            .forEach(session -> session.setRevokedAt(revokedAt));
    }
}
