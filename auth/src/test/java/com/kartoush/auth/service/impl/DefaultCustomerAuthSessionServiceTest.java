package com.kartoush.auth.service.impl;

import com.kartoush.auth.domain.IssuedCustomerAccessToken;
import com.kartoush.auth.persistence.entity.CustomerAuthSessionEntity;
import com.kartoush.auth.persistence.repository.CustomerAuthSessionRepository;
import com.kartoush.auth.service.CustomerAccessTokenGenerator;
import com.kartoush.auth.service.CustomerAccessTokenHasher;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.ulid.UlidGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCustomerAuthSessionServiceTest {

    private static final CustomerId CUSTOMER_ID =
        CustomerId.of("01J2Z5Y6K4Z6D5H2X3JH8M9N0P");
    private static final String SESSION_ID = "01JSESSIONID00000000000000";
    private static final String RAW_TOKEN = "opaque-token";
    private static final String TOKEN_HASH = "opaque-token-hash";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-29T18:00:00Z");

    @Mock
    private CustomerAuthSessionRepository customerAuthSessionRepository;

    @Mock
    private CustomerAccessTokenGenerator customerAccessTokenGenerator;

    @Mock
    private CustomerAccessTokenHasher customerAccessTokenHasher;

    @Mock
    private UlidGenerator ulidGenerator;

    @Mock
    private Clock clock;

    @InjectMocks
    private DefaultCustomerAuthSessionService customerAuthSessionService;

    @Test
    void shouldIssueCustomerAccessTokenAndPersistSession() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(customerAccessTokenGenerator.generate()).thenReturn(RAW_TOKEN);
        when(customerAccessTokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(ulidGenerator.next()).thenReturn(SESSION_ID);
        when(customerAuthSessionRepository.save(any(CustomerAuthSessionEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        final IssuedCustomerAccessToken issued =
            customerAuthSessionService.issueFor(CUSTOMER_ID);

        assertThat(issued.accessToken()).isEqualTo(RAW_TOKEN);
        assertThat(issued.tokenType()).isEqualTo("Bearer");
        verify(customerAuthSessionRepository).save(any(CustomerAuthSessionEntity.class));
    }
}
