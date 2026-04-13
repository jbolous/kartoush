package com.kartoush.customer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kartoush.customer.domain.ActivationToken;
import com.kartoush.customer.exception.ActivationTokenConsumedException;
import com.kartoush.customer.exception.ActivationTokenExpiredException;
import com.kartoush.customer.exception.ActivationTokenNotFoundException;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.persistence.entity.ActivationTokenEntity;
import com.kartoush.customer.persistence.mapper.ActivationTokenMapper;
import com.kartoush.customer.persistence.model.ActivationTokenIdEmbeddable;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.customer.persistence.repository.ActivationTokenRepository;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.customer.service.ActivationTokenGenerator;
import com.kartoush.customer.service.ActivationTokenHasher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.kartoush.platform.types.ActivationTokenId;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.ulid.UlidGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultActivationTokenServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-06T15:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final String RAW_TOKEN = "generated-raw-token";
    private static final String TOKEN_HASH = "generated-token-hash";
    private static final String EXISTING_TOKEN_HASH = "existing-token-hash";
    private static final String CUSTOMER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String ACTIVATION_TOKEN_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final long TWENTY_FOUR_HOURS_IN_SECONDS = 24 * 60 * 60;

    @Mock
    private ActivationTokenRepository activationTokenRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ActivationTokenGenerator activationTokenGenerator;

    @Mock
    private ActivationTokenHasher activationTokenHasher;

    @Mock
    private ActivationTokenMapper activationTokenMapper;

    @Mock
    private UlidGenerator ulidGenerator;

    @Captor
    private ArgumentCaptor<ActivationTokenEntity> activationTokenEntityCaptor;

    @Test
    void shouldCreateActivationTokenForCustomer() {
        // given
        CustomerId customerId = CustomerId.of(CUSTOMER_ID);

        DefaultActivationTokenService activationTokenService =
            new DefaultActivationTokenService(
                activationTokenRepository,
                customerRepository,
                activationTokenGenerator,
                activationTokenHasher,
                activationTokenMapper,
                ulidGenerator,
                FIXED_CLOCK);

        stubExistingCustomer(customerId);
        when(activationTokenGenerator.generate()).thenReturn(RAW_TOKEN);
        when(activationTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        stubGeneratedActivationTokenId();
        stubMapperToEntity();
        stubMapperToDomain();
        when(activationTokenRepository.save(activationTokenEntityCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ActivationToken activationToken = activationTokenService.createFor(customerId);

        // then
        verify(activationTokenGenerator).generate();
        verify(activationTokenHasher).hash(RAW_TOKEN);
        verify(activationTokenRepository).save(activationTokenEntityCaptor.getValue());

        ActivationTokenEntity savedActivationTokenEntity = activationTokenEntityCaptor.getValue();

        assertThat(savedActivationTokenEntity.getCustomerId()).isEqualTo(CustomerIdEmbeddable.from(customerId));
        assertThat(savedActivationTokenEntity.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(savedActivationTokenEntity.getCreatedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(savedActivationTokenEntity.getExpiresAt()).isEqualTo(FIXED_INSTANT.plusSeconds(TWENTY_FOUR_HOURS_IN_SECONDS));
        assertThat(savedActivationTokenEntity.getConsumedAt()).isNull();
        assertThat(savedActivationTokenEntity.getId()).isNotNull();

        assertThat(activationToken.getId()).isEqualTo(ActivationTokenId.of(ACTIVATION_TOKEN_ID));
        assertThat(activationToken.getCustomerId()).isEqualTo(customerId);
        assertThat(activationToken.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(activationToken.getCreatedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(activationToken.getExpiresAt()).isEqualTo(FIXED_INSTANT.plusSeconds(TWENTY_FOUR_HOURS_IN_SECONDS));
        assertThat(activationToken.getConsumedAt()).isNull();
    }

    @Test
    void shouldSetExpirationToTwentyFourHoursAfterCreation() {
        // given
        CustomerId customerId = CustomerId.of(CUSTOMER_ID);

        DefaultActivationTokenService activationTokenService =
            new DefaultActivationTokenService(
                activationTokenRepository,
                customerRepository,
                activationTokenGenerator,
                activationTokenHasher,
                activationTokenMapper,
                ulidGenerator,
                FIXED_CLOCK);

        stubExistingCustomer(customerId);
        when(activationTokenGenerator.generate()).thenReturn(RAW_TOKEN);
        when(activationTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        stubGeneratedActivationTokenId();
        stubMapperToEntity();
        when(activationTokenRepository.save(activationTokenEntityCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        activationTokenService.createFor(customerId);

        // then
        ActivationTokenEntity savedActivationToken = activationTokenEntityCaptor.getValue();
        assertThat(savedActivationToken.getExpiresAt())
            .isEqualTo(savedActivationToken.getCreatedAt().plusSeconds(TWENTY_FOUR_HOURS_IN_SECONDS));
    }

    @Test
    void shouldHashGeneratedRawTokenBeforeSaving() {
        // given
        CustomerId customerId = CustomerId.of(CUSTOMER_ID);

        DefaultActivationTokenService activationTokenService =
            new DefaultActivationTokenService(
                activationTokenRepository,
                customerRepository,
                activationTokenGenerator,
                activationTokenHasher,
                activationTokenMapper,
                ulidGenerator,
                FIXED_CLOCK);

        stubExistingCustomer(customerId);
        when(activationTokenGenerator.generate()).thenReturn(RAW_TOKEN);
        when(activationTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        stubGeneratedActivationTokenId();
        stubMapperToEntity();
        when(activationTokenRepository.save(activationTokenEntityCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        activationTokenService.createFor(customerId);

        // then
        verify(activationTokenHasher).hash(RAW_TOKEN);

        ActivationTokenEntity savedActivationToken = activationTokenEntityCaptor.getValue();
        assertThat(savedActivationToken.getTokenHash()).isEqualTo(TOKEN_HASH);
    }

    @Test
    void shouldGenerateHashAndSaveOnlyOnce() {
        // given
        CustomerId customerId = CustomerId.of(CUSTOMER_ID);

        DefaultActivationTokenService activationTokenService =
            new DefaultActivationTokenService(
                activationTokenRepository,
                customerRepository,
                activationTokenGenerator,
                activationTokenHasher,
                activationTokenMapper,
                ulidGenerator,
                FIXED_CLOCK);

        stubExistingCustomer(customerId);
        when(activationTokenGenerator.generate()).thenReturn(RAW_TOKEN);
        when(activationTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        stubGeneratedActivationTokenId();
        stubMapperToEntity();
        when(activationTokenRepository.save(activationTokenEntityCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        activationTokenService.createFor(customerId);

        // then
        verify(activationTokenGenerator, times(1)).generate();
        verify(activationTokenHasher, times(1)).hash(RAW_TOKEN);
        verify(activationTokenRepository, times(1)).save(activationTokenEntityCaptor.getValue());
    }

    @Test
    void shouldThrowWhenCustomerDoesNotExist() {
        // given
        CustomerId customerId = CustomerId.of(CUSTOMER_ID);

        DefaultActivationTokenService activationTokenService =
            new DefaultActivationTokenService(
                activationTokenRepository,
                customerRepository,
                activationTokenGenerator,
                activationTokenHasher,
                activationTokenMapper,
                ulidGenerator,
                FIXED_CLOCK);

        when(customerRepository.existsById(CustomerIdEmbeddable.from(customerId))).thenReturn(false);

        // when/then
        assertThatThrownBy(() -> activationTokenService.createFor(customerId))
            .isInstanceOf(CustomerNotFoundException.class)
            .hasMessage("Customer not found for id: " + customerId.value());

        verify(activationTokenGenerator, never()).generate();
        verify(activationTokenHasher, never()).hash(any());
        verify(activationTokenRepository, never()).save(any());
    }

    @Test
    void shouldValidateExistingActivationToken() {
        // given
        CustomerId customerId = CustomerId.of(CUSTOMER_ID);

        DefaultActivationTokenService activationTokenService =
            new DefaultActivationTokenService(
                activationTokenRepository,
                customerRepository,
                activationTokenGenerator,
                activationTokenHasher,
                activationTokenMapper,
                ulidGenerator,
                FIXED_CLOCK);

        ActivationToken activationToken = ActivationToken.fromPersistence(
            ActivationTokenId.of(ACTIVATION_TOKEN_ID),
            customerId,
            TOKEN_HASH,
            FIXED_INSTANT.plusSeconds(TWENTY_FOUR_HOURS_IN_SECONDS),
            null,
            FIXED_INSTANT);

        when(activationTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(activationTokenRepository.findByCustomerIdAndTokenHash(CustomerIdEmbeddable.from(customerId), TOKEN_HASH))
            .thenReturn(java.util.Optional.of(ActivationTokenEntity.of(
                ActivationTokenIdEmbeddable.from(activationToken.getId()),
                CustomerIdEmbeddable.from(activationToken.getCustomerId()),
                activationToken.getTokenHash(),
                activationToken.getExpiresAt(),
                activationToken.getConsumedAt(),
                activationToken.getCreatedAt()
            )));
        stubMapperToDomain();

        // when
        ActivationToken validatedToken = activationTokenService.validate(customerId, RAW_TOKEN);

        // then
        verify(activationTokenHasher).hash(RAW_TOKEN);
        verify(activationTokenRepository).findByCustomerIdAndTokenHash(CustomerIdEmbeddable.from(customerId), TOKEN_HASH);
        assertThat(validatedToken.getId()).isEqualTo(activationToken.getId());
        assertThat(validatedToken.getCustomerId()).isEqualTo(customerId);
        assertThat(validatedToken.getTokenHash()).isEqualTo(TOKEN_HASH);
    }

    @Test
    void shouldThrowWhenActivationTokenDoesNotExist() {
        // given
        CustomerId customerId = CustomerId.of(CUSTOMER_ID);

        DefaultActivationTokenService activationTokenService =
            new DefaultActivationTokenService(
                activationTokenRepository,
                customerRepository,
                activationTokenGenerator,
                activationTokenHasher,
                activationTokenMapper,
                ulidGenerator,
                FIXED_CLOCK);

        when(activationTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(activationTokenRepository.findByCustomerIdAndTokenHash(CustomerIdEmbeddable.from(customerId), TOKEN_HASH))
            .thenReturn(java.util.Optional.empty());

        // when/then
        assertThatThrownBy(() -> activationTokenService.validate(customerId, RAW_TOKEN))
            .isInstanceOf(ActivationTokenNotFoundException.class)
            .hasMessage("Activation token not found for customer id: " + customerId.value());
    }

    @Test
    void shouldThrowWhenActivationTokenIsNull() {
        // given
        CustomerId customerId = CustomerId.of(CUSTOMER_ID);

        DefaultActivationTokenService activationTokenService =
            new DefaultActivationTokenService(
                activationTokenRepository,
                customerRepository,
                activationTokenGenerator,
                activationTokenHasher,
                activationTokenMapper,
                ulidGenerator,
                FIXED_CLOCK);

        // when/then
        assertThatThrownBy(() -> activationTokenService.validate(customerId, null))
            .isInstanceOf(ActivationTokenNotFoundException.class)
            .hasMessage("Activation token not found for customer id: " + customerId.value());

        verify(activationTokenHasher, never()).hash(any());
        verify(activationTokenRepository, never()).findByCustomerIdAndTokenHash(any(), any());
    }

    @Test
    void shouldThrowWhenActivationTokenIsBlank() {
        // given
        CustomerId customerId = CustomerId.of(CUSTOMER_ID);

        DefaultActivationTokenService activationTokenService =
            new DefaultActivationTokenService(
                activationTokenRepository,
                customerRepository,
                activationTokenGenerator,
                activationTokenHasher,
                activationTokenMapper,
                ulidGenerator,
                FIXED_CLOCK);

        // when/then
        assertThatThrownBy(() -> activationTokenService.validate(customerId, "   "))
            .isInstanceOf(ActivationTokenNotFoundException.class)
            .hasMessage("Activation token not found for customer id: " + customerId.value());

        verify(activationTokenHasher, never()).hash(any());
        verify(activationTokenRepository, never()).findByCustomerIdAndTokenHash(any(), any());
    }

    @Test
    void shouldThrowWhenActivationTokenIsExpired() {
        // given
        CustomerId customerId = CustomerId.of(CUSTOMER_ID);

        DefaultActivationTokenService activationTokenService =
            new DefaultActivationTokenService(
                activationTokenRepository,
                customerRepository,
                activationTokenGenerator,
                activationTokenHasher,
                activationTokenMapper,
                ulidGenerator,
                FIXED_CLOCK);

        ActivationToken expiredToken = ActivationToken.fromPersistence(
            ActivationTokenId.of(ACTIVATION_TOKEN_ID),
            customerId,
            TOKEN_HASH,
            FIXED_INSTANT.minusSeconds(1),
            null,
            FIXED_INSTANT.minusSeconds(TWENTY_FOUR_HOURS_IN_SECONDS));

        when(activationTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(activationTokenRepository.findByCustomerIdAndTokenHash(CustomerIdEmbeddable.from(customerId), TOKEN_HASH))
            .thenReturn(java.util.Optional.of(ActivationTokenEntity.of(
                ActivationTokenIdEmbeddable.from(expiredToken.getId()),
                CustomerIdEmbeddable.from(expiredToken.getCustomerId()),
                expiredToken.getTokenHash(),
                expiredToken.getExpiresAt(),
                expiredToken.getConsumedAt(),
                expiredToken.getCreatedAt()
            )));
        stubMapperToDomain();

        // when/then
        assertThatThrownBy(() -> activationTokenService.validate(customerId, RAW_TOKEN))
            .isInstanceOf(ActivationTokenExpiredException.class)
            .hasMessage("Activation token is expired for customer id: " + customerId.value());
    }

    @Test
    void shouldThrowWhenActivationTokenIsConsumed() {
        // given
        CustomerId customerId = CustomerId.of(CUSTOMER_ID);

        DefaultActivationTokenService activationTokenService =
            new DefaultActivationTokenService(
                activationTokenRepository,
                customerRepository,
                activationTokenGenerator,
                activationTokenHasher,
                activationTokenMapper,
                ulidGenerator,
                FIXED_CLOCK);

        ActivationToken consumedToken = ActivationToken.fromPersistence(
            ActivationTokenId.of(ACTIVATION_TOKEN_ID),
            customerId,
            TOKEN_HASH,
            FIXED_INSTANT.plusSeconds(TWENTY_FOUR_HOURS_IN_SECONDS),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT);

        when(activationTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(activationTokenRepository.findByCustomerIdAndTokenHash(CustomerIdEmbeddable.from(customerId), TOKEN_HASH))
            .thenReturn(java.util.Optional.of(ActivationTokenEntity.of(
                ActivationTokenIdEmbeddable.from(consumedToken.getId()),
                CustomerIdEmbeddable.from(consumedToken.getCustomerId()),
                consumedToken.getTokenHash(),
                consumedToken.getExpiresAt(),
                consumedToken.getConsumedAt(),
                consumedToken.getCreatedAt()
            )));
        stubMapperToDomain();

        // when/then
        assertThatThrownBy(() -> activationTokenService.validate(customerId, RAW_TOKEN))
            .isInstanceOf(ActivationTokenConsumedException.class)
            .hasMessage("Activation token has already been consumed for customer id: " + customerId.value());
    }

    @Test
    void shouldConsumeActivationToken() {
        // given
        CustomerId customerId = CustomerId.of(CUSTOMER_ID);

        DefaultActivationTokenService activationTokenService =
            new DefaultActivationTokenService(
                activationTokenRepository,
                customerRepository,
                activationTokenGenerator,
                activationTokenHasher,
                activationTokenMapper,
                ulidGenerator,
                FIXED_CLOCK);

        ActivationToken activationToken = ActivationToken.fromPersistence(
            ActivationTokenId.of(ACTIVATION_TOKEN_ID),
            customerId,
            TOKEN_HASH,
            FIXED_INSTANT.plusSeconds(TWENTY_FOUR_HOURS_IN_SECONDS),
            null,
            FIXED_INSTANT);

        stubMapperToEntity();
        stubMapperToDomain();
        when(activationTokenRepository.save(activationTokenEntityCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ActivationToken consumedActivationToken = activationTokenService.consume(activationToken);

        // then
        verify(activationTokenRepository).save(activationTokenEntityCaptor.getValue());
        assertThat(consumedActivationToken.getConsumedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(activationTokenEntityCaptor.getValue().getConsumedAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void shouldResendActivationTokenAndInvalidateExistingTokens() {
        // given
        CustomerId customerId = CustomerId.of(CUSTOMER_ID);
        ActivationTokenEntity existingToken = ActivationTokenEntity.of(
            ActivationTokenIdEmbeddable.from(ActivationTokenId.of(ACTIVATION_TOKEN_ID)),
            CustomerIdEmbeddable.from(customerId),
            EXISTING_TOKEN_HASH,
            FIXED_INSTANT.plusSeconds(TWENTY_FOUR_HOURS_IN_SECONDS),
            null,
            FIXED_INSTANT.minusSeconds(60));

        DefaultActivationTokenService activationTokenService =
            new DefaultActivationTokenService(
                activationTokenRepository,
                customerRepository,
                activationTokenGenerator,
                activationTokenHasher,
                activationTokenMapper,
                ulidGenerator,
                FIXED_CLOCK);

        stubExistingCustomer(customerId);
        when(activationTokenRepository.findAllByCustomerIdAndConsumedAtIsNull(CustomerIdEmbeddable.from(customerId)))
            .thenReturn(List.of(existingToken));
        when(activationTokenRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(activationTokenGenerator.generate()).thenReturn(RAW_TOKEN);
        when(activationTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        stubGeneratedActivationTokenId();
        stubMapperToEntity();
        stubMapperToDomain();
        when(activationTokenRepository.save(activationTokenEntityCaptor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ActivationToken resentToken = activationTokenService.resendFor(customerId);

        // then
        verify(activationTokenRepository).findAllByCustomerIdAndConsumedAtIsNull(CustomerIdEmbeddable.from(customerId));
        verify(activationTokenRepository).saveAll(any());
        assertThat(existingToken.getConsumedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(resentToken.getTokenHash()).isEqualTo(TOKEN_HASH);
    }

    private void stubExistingCustomer(CustomerId customerId) {
        when(customerRepository.existsById(CustomerIdEmbeddable.from(customerId))).thenReturn(true);
    }

    private void stubGeneratedActivationTokenId() {
        when(ulidGenerator.next()).thenReturn(ACTIVATION_TOKEN_ID);
    }

    private void stubMapperToEntity() {
        when(activationTokenMapper.toEntity(any(ActivationToken.class)))
            .thenAnswer(invocation -> {
                ActivationToken activationToken = invocation.getArgument(0);

                return ActivationTokenEntity.of(
                    ActivationTokenIdEmbeddable.from(activationToken.getId()),
                    CustomerIdEmbeddable.from(activationToken.getCustomerId()),
                    activationToken.getTokenHash(),
                    activationToken.getExpiresAt(),
                    activationToken.getConsumedAt(),
                    activationToken.getCreatedAt()
                );
            });
    }

    private void stubMapperToDomain() {
        when(activationTokenMapper.toDomain(any(ActivationTokenEntity.class)))
            .thenAnswer(invocation -> {
                ActivationTokenEntity entity = invocation.getArgument(0);

                return ActivationToken.fromPersistence(
                    entity.getId().toActivationTokenId(),
                    entity.getCustomerId().toCustomerId(),
                    entity.getTokenHash(),
                    entity.getExpiresAt(),
                    entity.getConsumedAt(),
                    entity.getCreatedAt()
                );
            });
    }
}
