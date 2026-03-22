package com.kartoush.customer.persistence;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.entity.CustomerProfileEntity;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.customer.test.CustomerTestApplication;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.testsupport.IntegrationTest;
import com.kartoush.testsupport.PostgresSpringIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@IntegrationTest
@ContextConfiguration(classes = CustomerTestApplication.class)
@Import(CustomerJpaTestConfig.class)
class CustomerEntityRepositoryTest extends PostgresSpringIntegrationTest {

    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "555-555-1212";
    private static final String EMAIL = "jack@kartoush.test";
    private static final String PASSWORD_HASH = "ABCXYZ123789";

    @Autowired
    private UlidGenerator ulidGenerator;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("New customer should receive identifier and audit timestamps when persisted")
    void shouldPersistAndLoadCustomer() {

        // given
        CustomerIdEmbeddable id = CustomerIdEmbeddable.from(CustomerId.newId(ulidGenerator));
        CustomerProfileEntity profile = new CustomerProfileEntity(FIRST_NAME, LAST_NAME, PHONE_NUMBER);
        CustomerEntity customer = CustomerEntity
                .newCustomer(id,
                        profile,
                        EMAIL,
                        PASSWORD_HASH,
                        CustomerStatus.ACTIVE
                        );

        // when
        CustomerEntity saved = customerRepository.saveAndFlush(customer);
        CustomerProfileEntity savedProfile = saved.getProfile();

        // then (save side)
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getEmail()).isNotNull();
        assertThat(saved.getProfile()).isNotNull();
        assertThat(saved.getProfile().getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(saved.getProfile().getLastName()).isEqualTo(LAST_NAME);
        assertThat(saved.getProfile().getPhoneNumber()).isEqualTo(PHONE_NUMBER);

        // when
        CustomerIdEmbeddable savedId = saved.getCustomerId();
        Optional<CustomerEntity> retrieved = customerRepository.findById(savedId);

        // then (load side)
        assertThat(retrieved).isPresent();
        CustomerProfileEntity retrievedProfile = retrieved.get().getProfile();

        assertThat(retrieved.get().getId()).isEqualTo(savedId.getValue());
        assertThat(retrieved.get().getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(retrieved.get().getUpdatedAt()).isEqualTo(saved.getUpdatedAt());
        assertThat(saved.getEmail()).isEqualTo(EMAIL.toLowerCase(Locale.ROOT));

        assertThat(retrievedProfile.getFirstName()).isEqualTo(savedProfile.getFirstName());
        assertThat(retrievedProfile.getLastName()).isEqualTo(savedProfile.getLastName());
        assertThat(retrievedProfile.getPhoneNumber()).isEqualTo(savedProfile.getPhoneNumber());

    }

    @Test
    @DisplayName("Customer should preserve provided identifier when persisted")
    void shouldPreserveProvidedCustomerIdWhenProvided() {

        // given
        CustomerIdEmbeddable id = CustomerIdEmbeddable.from(CustomerId.newId(ulidGenerator));
        CustomerProfileEntity profile = new CustomerProfileEntity(FIRST_NAME, LAST_NAME, PHONE_NUMBER);
        CustomerEntity customerEntity = CustomerEntity
                .newCustomer(id,
                        profile,
                        EMAIL,
                        PASSWORD_HASH,
                        CustomerStatus.ACTIVE
                );

        // when
        CustomerEntity saved = customerRepository.saveAndFlush(customerEntity);

        // then
        assertThat(saved.getId()).isEqualTo(id.getValue());
    }

    @Test
    void shouldAllowReusingOriginalEmailAfterSoftDelete()
    {
        // given
        final String originalEmail = "jack@kartoush.test";
        final CustomerIdEmbeddable customerId1 = CustomerIdEmbeddable.from(CustomerId.newId(ulidGenerator));
        final CustomerProfileEntity profile = new CustomerProfileEntity(FIRST_NAME, LAST_NAME, PHONE_NUMBER);

        final CustomerEntity first = CustomerEntity.newCustomer(
            customerId1,
            profile,
            originalEmail,
            PASSWORD_HASH,
            CustomerStatus.PENDING);

        // persist original
        customerRepository.saveAndFlush(first);

        // simulate soft delete
        first.setEmail(originalEmail + "|deleted|" + customerId1.getValue().toLowerCase(Locale.ROOT));
        first.setCustomerStatus(CustomerStatus.DELETED);

        customerRepository.saveAndFlush(first);

        final CustomerIdEmbeddable customerId2 = CustomerIdEmbeddable.from(CustomerId.newId(ulidGenerator));
        final CustomerProfileEntity profile2 = new CustomerProfileEntity(FIRST_NAME, LAST_NAME, PHONE_NUMBER);

        final CustomerEntity second = CustomerEntity.newCustomer(
            customerId2,
            profile2,
            originalEmail,
            PASSWORD_HASH,
            CustomerStatus.PENDING
        );

        // create new customer with same email
        customerRepository.saveAndFlush(second);

        // then
        final List<CustomerEntity> customers = customerRepository.findAll();

        assertThat(customers).hasSize(2);

        final CustomerEntity deleted = customerRepository.findById(customerId1).orElseThrow();
        final CustomerEntity active = customerRepository.findById(customerId2).orElseThrow();

        assertThat(deleted.getCustomerStatus()).isEqualTo(CustomerStatus.DELETED);
        assertThat(deleted.getEmail())
            .isEqualTo(originalEmail + "|deleted|" + customerId1.getValue().toLowerCase(Locale.ROOT));

        assertThat(active.getCustomerStatus()).isEqualTo(CustomerStatus.PENDING);
        assertThat(active.getEmail()).isEqualTo(originalEmail);
    }
}
