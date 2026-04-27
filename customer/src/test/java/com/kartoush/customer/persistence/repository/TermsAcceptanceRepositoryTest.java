package com.kartoush.customer.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kartoush.customer.CustomerTestApplication;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.entity.CustomerProfileEntity;
import com.kartoush.customer.persistence.entity.TermsAcceptanceEntity;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.ulid.DefaultUlidGenerator;
import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.testsupport.IntegrationTest;
import com.kartoush.testsupport.PostgresDataJpaTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

@IntegrationTest
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CustomerTestApplication.class)
class TermsAcceptanceRepositoryTest extends PostgresDataJpaTest {

    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "555-555-1212";
    private static final String EMAIL_PREFIX = "jack";
    private static final String EMAIL_SUFFIX = "@kartoush.com";
    private static final String TERMS_VERSION = "2026.04.01";
    private static final Instant ACCEPTED_AT = Instant.parse("2026-04-18T18:00:00Z");

    private final UlidGenerator ulidGenerator = new DefaultUlidGenerator();

    @Autowired
    private TermsAcceptanceRepository termsAcceptanceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldPersistAndLoadTermsAcceptance() {
        // given
        final CustomerEntity customer = saveCustomer();
        final String acceptanceId = ulidGenerator.next();
        final TermsAcceptanceEntity termsAcceptance = TermsAcceptanceEntity.of(
            acceptanceId,
            customer.getCustomerId(),
            TERMS_VERSION,
            ACCEPTED_AT
        );

        // when
        final TermsAcceptanceEntity saved = termsAcceptanceRepository.saveAndFlush(termsAcceptance);
        final List<TermsAcceptanceEntity> retrieved =
            termsAcceptanceRepository.findAllByCustomerIdOrderByAcceptedAtAsc(customer.getCustomerId());

        // then
        assertThat(saved.getId()).isEqualTo(acceptanceId);
        assertThat(saved.getCustomerId()).isEqualTo(customer.getCustomerId());
        assertThat(saved.getTermsVersion()).isEqualTo(TERMS_VERSION);
        assertThat(saved.getAcceptedAt()).isEqualTo(ACCEPTED_AT);

        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.getFirst().getId()).isEqualTo(acceptanceId);
        assertThat(retrieved.getFirst().getCustomerId()).isEqualTo(customer.getCustomerId());
        assertThat(retrieved.getFirst().getTermsVersion()).isEqualTo(TERMS_VERSION);
        assertThat(retrieved.getFirst().getAcceptedAt()).isEqualTo(ACCEPTED_AT);
    }

    private CustomerEntity saveCustomer() {
        final CustomerIdEmbeddable customerId = CustomerIdEmbeddable.from(CustomerId.newId(ulidGenerator));
        final CustomerProfileEntity profile = new CustomerProfileEntity(FIRST_NAME, LAST_NAME, PHONE_NUMBER);
        final CustomerEntity customer = CustomerEntity.newCustomer(
            customerId,
            profile,
            EMAIL_PREFIX + customerId.getValue() + EMAIL_SUFFIX,
            CustomerStatus.PENDING
        );

        return customerRepository.saveAndFlush(customer);
    }
}
