package com.kartoush.auth.service.impl;

import com.kartoush.auth.domain.IssuedCustomerAccessToken;
import com.kartoush.auth.persistence.entity.CustomerAuthSessionEntity;
import com.kartoush.auth.persistence.repository.CustomerAuthSessionRepository;
import com.kartoush.auth.service.CustomerAccessTokenGenerator;
import com.kartoush.auth.service.CustomerAccessTokenHasher;
import com.kartoush.auth.service.CustomerAuthSessionService;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.ulid.UlidGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class DefaultCustomerAuthSessionService implements CustomerAuthSessionService {

    private static final String TOKEN_TYPE = "Bearer";

    private final CustomerAuthSessionRepository customerAuthSessionRepository;
    private final CustomerAccessTokenGenerator customerAccessTokenGenerator;
    private final CustomerAccessTokenHasher customerAccessTokenHasher;
    private final UlidGenerator ulidGenerator;
    private final Clock clock;

    public DefaultCustomerAuthSessionService(
        final CustomerAuthSessionRepository customerAuthSessionRepository,
        final CustomerAccessTokenGenerator customerAccessTokenGenerator,
        final CustomerAccessTokenHasher customerAccessTokenHasher,
        final UlidGenerator ulidGenerator,
        final Clock clock
    ) {
        this.customerAuthSessionRepository = customerAuthSessionRepository;
        this.customerAccessTokenGenerator = customerAccessTokenGenerator;
        this.customerAccessTokenHasher = customerAccessTokenHasher;
        this.ulidGenerator = ulidGenerator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IssuedCustomerAccessToken issueFor(final CustomerId customerId) {
        final Instant issuedAt = Instant.now(clock);
        final String rawToken = customerAccessTokenGenerator.generate();
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
}
