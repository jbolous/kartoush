package com.kartoush.customer.service.impl;

import com.kartoush.customer.domain.ActivationToken;
import com.kartoush.customer.exception.ActivationTokenConsumedException;
import com.kartoush.customer.exception.ActivationTokenExpiredException;
import com.kartoush.customer.exception.ActivationTokenNotFoundException;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.persistence.entity.ActivationTokenEntity;
import com.kartoush.customer.persistence.mapper.ActivationTokenMapper;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.customer.persistence.repository.ActivationTokenRepository;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.customer.service.ActivationTokenGenerator;
import com.kartoush.customer.service.ActivationTokenHasher;
import com.kartoush.customer.service.ActivationTokenService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.kartoush.platform.types.ActivationTokenId;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.ulid.UlidGenerator;
import org.springframework.stereotype.Service;

@Service
public class DefaultActivationTokenService implements ActivationTokenService {

    private static final Duration ACTIVATION_TOKEN_TTL = Duration.ofHours(24);

    private final ActivationTokenRepository activationTokenRepository;
    private final CustomerRepository customerRepository;
    private final ActivationTokenGenerator activationTokenGenerator;
    private final ActivationTokenHasher activationTokenHasher;
    private final ActivationTokenMapper activationTokenMapper;
    private final UlidGenerator ulidGenerator;
    private final Clock clock;

    public DefaultActivationTokenService(
        ActivationTokenRepository activationTokenRepository,
        CustomerRepository customerRepository,
        ActivationTokenGenerator activationTokenGenerator,
        ActivationTokenHasher activationTokenHasher,
        ActivationTokenMapper activationTokenMapper,
        UlidGenerator ulidGenerator,
        Clock clock) {
        this.activationTokenRepository = activationTokenRepository;
        this.customerRepository = customerRepository;
        this.activationTokenGenerator = activationTokenGenerator;
        this.activationTokenHasher = activationTokenHasher;
        this.activationTokenMapper = activationTokenMapper;
        this.ulidGenerator = ulidGenerator;
        this.clock = clock;
    }

    @Override
    public ActivationToken createFor(CustomerId customerId) {
        if (!customerRepository.existsById(CustomerIdEmbeddable.from(customerId))) {
            throw new CustomerNotFoundException(customerId.value());
        }

        Instant createdAt = Instant.now(clock);
        Instant expiresAt = createdAt.plus(ACTIVATION_TOKEN_TTL);

        String rawToken = activationTokenGenerator.generate();
        String tokenHash = activationTokenHasher.hash(rawToken);

        ActivationToken activationToken = ActivationToken.of(
            ActivationTokenId.newId(ulidGenerator),
            customerId,
            tokenHash,
            expiresAt,
            null,
            createdAt);

        ActivationTokenEntity activationTokenEntity = activationTokenMapper.toEntity(activationToken);

        return activationTokenMapper.toDomain(activationTokenRepository.save(activationTokenEntity));
    }

    @Override
    public ActivationToken validate(CustomerId customerId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ActivationTokenNotFoundException(customerId.value());
        }

        String tokenHash = activationTokenHasher.hash(rawToken);

        ActivationToken activationToken = activationTokenRepository.findByCustomerIdAndTokenHash(
                CustomerIdEmbeddable.from(customerId),
                tokenHash)
            .map(activationTokenMapper::toDomain)
            .orElseThrow(() -> new ActivationTokenNotFoundException(customerId.value()));

        Instant now = Instant.now(clock);

        if (activationToken.isConsumed()) {
            throw new ActivationTokenConsumedException(customerId.value());
        }

        if (activationToken.isExpired(now)) {
            throw new ActivationTokenExpiredException(customerId.value());
        }

        return activationToken;
    }
}
