package com.kartoush.customer.service.impl;

import com.kartoush.customer.CustomerTestApplication;
import com.kartoush.customer.persistence.entity.ActivationTokenEntity;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.entity.CustomerProfileEntity;
import com.kartoush.customer.persistence.mapper.ActivationTokenMapper;
import com.kartoush.customer.persistence.model.ActivationTokenIdEmbeddable;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.customer.persistence.repository.ActivationTokenRepository;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.customer.service.ActivationTokenGenerator;
import com.kartoush.customer.service.ActivationTokenHasher;
import com.kartoush.customer.service.ActivationTokenService;
import com.kartoush.platform.types.ActivationTokenId;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.ulid.DefaultUlidGenerator;
import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.testsupport.IntegrationTest;
import com.kartoush.testsupport.PostgresDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@IntegrationTest
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CustomerTestApplication.class)
@Import({DefaultActivationTokenService.class, ActivationTokenMapper.class,
    ActivationTokenResendTransactionIntegrationTest.Dependencies.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ActivationTokenResendTransactionIntegrationTest extends PostgresDataJpaTest {
    @Autowired private ActivationTokenService service;
    @Autowired private ActivationTokenRepository tokens;
    @Autowired private CustomerRepository customers;
    @Autowired private UlidGenerator ids;
    @Autowired private PlatformTransactionManager manager;
    @MockitoBean private ActivationTokenGenerator generator;
    @MockitoBean private ActivationTokenHasher hasher;

    @Test
    void shouldRestoreOldTokensWhenDirectResendFailsAfterConsumptionIsFlushed() {
        final var original = seed();
        when(generator.generate()).thenAnswer(invocation -> {
            tokens.flush();
            throw new IllegalStateException("replacement generation failed");
        });

        assertThatThrownBy(() -> service.resendFor(original.getCustomerId().toCustomerId()))
            .hasMessage("replacement generation failed");

        assertThat(tokens.findById(original.getId()).orElseThrow().getConsumedAt()).isNull();
        assertThat(tokens.findAllByCustomerIdAndConsumedAtIsNull(original.getCustomerId())).hasSize(1);
    }

    @Test
    void shouldCommitConsumptionAndReplacementWhenDirectResendSucceeds() {
        final var original = seed();
        stubReplacement();
        final var issued = service.resendFor(original.getCustomerId().toCustomerId());
        assertThat(tokens.findById(original.getId()).orElseThrow().getConsumedAt()).isNotNull();
        assertThat(tokens.findAllByCustomerIdAndConsumedAtIsNull(original.getCustomerId()))
            .singleElement().satisfies(token ->
                assertThat(token.getId().toActivationTokenId()).isEqualTo(issued.activationToken().getId()));
    }

    @Test
    void shouldJoinCallingTransactionWhenOuterWorkflowRollsBack() {
        final var original = seed();
        stubReplacement();
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            service.resendFor(original.getCustomerId().toCustomerId());
            tokens.flush();
            status.setRollbackOnly();
        });
        assertThat(tokens.findById(original.getId()).orElseThrow().getConsumedAt()).isNull();
        assertThat(tokens.findAllByCustomerIdAndConsumedAtIsNull(original.getCustomerId())).hasSize(1);
    }

    private void stubReplacement() {
        final String token = UUID.randomUUID().toString();
        when(generator.generate()).thenReturn(token);
        when(hasher.hash(token)).thenReturn("replacement-" + token);
    }

    private ActivationTokenEntity seed() {
        final var customerId = CustomerIdEmbeddable.from(CustomerId.newId(ids));
        customers.saveAndFlush(CustomerEntity.newCustomer(customerId,
            new CustomerProfileEntity("Test", "Customer", "+13125550001"),
            customerId.getValue() + "@example.com", CustomerStatus.PENDING));
        final var now = Instant.now();
        return tokens.saveAndFlush(ActivationTokenEntity.of(
            ActivationTokenIdEmbeddable.from(ActivationTokenId.newId(ids)), customerId,
            "original-" + UUID.randomUUID(), now.plusSeconds(3600), null, now));
    }

    @TestConfiguration
    static class Dependencies {
        @Bean Clock clock() { return Clock.systemUTC(); }
        @Bean UlidGenerator ulidGenerator() { return new DefaultUlidGenerator(); }
    }
}
