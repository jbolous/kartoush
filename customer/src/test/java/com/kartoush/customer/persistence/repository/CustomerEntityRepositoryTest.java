package com.kartoush.customer.persistence.repository;

import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.entity.CustomerProfileEntity;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.customer.CustomerTestApplication;
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
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CustomerTestApplication.class)
class CustomerEntityRepositoryTest extends PostgresDataJpaTest {

    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "555-555-1212";
    private static final String EMAIL = "jack@kartoush.test";
    private final UlidGenerator ulidGenerator = new DefaultUlidGenerator();

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldPersistAndLoadCustomer() {
        // given
        CustomerIdEmbeddable id = CustomerIdEmbeddable.from(CustomerId.newId(ulidGenerator));
        CustomerProfileEntity profile = new CustomerProfileEntity(FIRST_NAME, LAST_NAME, PHONE_NUMBER);
        CustomerEntity customer = CustomerEntity.newCustomer(
            id,
            profile,
            EMAIL,
            CustomerStatus.ACTIVE);

        CustomerEntity saved = customerRepository.saveAndFlush(customer);
        CustomerProfileEntity savedProfile = saved.getProfile();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getEmail()).isNotNull();
        assertThat(saved.getProfile()).isNotNull();
        assertThat(saved.getProfile().getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(saved.getProfile().getLastName()).isEqualTo(LAST_NAME);
        assertThat(saved.getProfile().getPhoneNumber()).isEqualTo(PHONE_NUMBER);

        CustomerIdEmbeddable savedId = saved.getCustomerId();
        Optional<CustomerEntity> retrieved = customerRepository.findById(savedId);

        // when
        assertThat(retrieved).isPresent();
        CustomerProfileEntity retrievedProfile = retrieved.get().getProfile();

        // then
        assertThat(retrieved.get().getId()).isEqualTo(savedId.getValue());
        assertThat(retrieved.get().getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(retrieved.get().getUpdatedAt()).isEqualTo(saved.getUpdatedAt());
        assertThat(saved.getEmail()).isEqualTo(EMAIL.toLowerCase(Locale.ROOT));

        assertThat(retrievedProfile.getFirstName()).isEqualTo(savedProfile.getFirstName());
        assertThat(retrievedProfile.getLastName()).isEqualTo(savedProfile.getLastName());
        assertThat(retrievedProfile.getPhoneNumber()).isEqualTo(savedProfile.getPhoneNumber());
    }

    @Test
    void shouldPreserveProvidedCustomerIdWhenProvided() {
        // given
        CustomerIdEmbeddable id = CustomerIdEmbeddable.from(CustomerId.newId(ulidGenerator));
        CustomerProfileEntity profile = new CustomerProfileEntity(FIRST_NAME, LAST_NAME, PHONE_NUMBER);
        CustomerEntity customerEntity = CustomerEntity.newCustomer(
            id,
            profile,
            EMAIL,
            CustomerStatus.ACTIVE);

        // when
        CustomerEntity saved = customerRepository.saveAndFlush(customerEntity);

        // then
        assertThat(saved.getId()).isEqualTo(id.getValue());
    }

    @Test
    void shouldAllowReusingOriginalEmailAfterSoftDelete() {
        // given
        final String originalEmail = EMAIL;
        final CustomerIdEmbeddable customerId1 = CustomerIdEmbeddable.from(CustomerId.newId(ulidGenerator));
        final CustomerProfileEntity profile = new CustomerProfileEntity(FIRST_NAME, LAST_NAME, PHONE_NUMBER);

        final CustomerEntity first = CustomerEntity.newCustomer(
            customerId1,
            profile,
            originalEmail,
            CustomerStatus.PENDING);

        customerRepository.saveAndFlush(first);

        first.setEmail(originalEmail + "|deleted|" + customerId1.getValue().toLowerCase(Locale.ROOT));
        first.setCustomerStatus(CustomerStatus.DELETED);

        // when
        customerRepository.saveAndFlush(first);

        final CustomerIdEmbeddable customerId2 = CustomerIdEmbeddable.from(CustomerId.newId(ulidGenerator));
        final CustomerProfileEntity profile2 = new CustomerProfileEntity(FIRST_NAME, LAST_NAME, PHONE_NUMBER);

        final CustomerEntity second = CustomerEntity.newCustomer(
            customerId2,
            profile2,
            originalEmail,
            CustomerStatus.PENDING);

        customerRepository.saveAndFlush(second);

        final List<CustomerEntity> customers = customerRepository.findAll();

        // then
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
