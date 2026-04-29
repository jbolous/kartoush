package com.kartoush.auth.persistence.repository;

import com.kartoush.auth.AuthTestApplication;
import com.kartoush.auth.persistence.entity.CustomerPasswordEntity;
import com.kartoush.testsupport.IntegrationTest;
import com.kartoush.testsupport.PostgresDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = AuthTestApplication.class)
class CustomerPasswordRepositoryTest extends PostgresDataJpaTest {

    private static final String CUSTOMER_ID = "01J2Z5Y6K4Z6D5H2X3JH8M9N0P";
    private static final String PASSWORD_HASH = "hash";

    @Autowired
    private CustomerPasswordRepository customerPasswordRepository;

    @Test
    void shouldPersistAndLoadCredential() {
        final CustomerPasswordEntity entity = CustomerPasswordEntity.create(CUSTOMER_ID, PASSWORD_HASH);

        final CustomerPasswordEntity saved = customerPasswordRepository.saveAndFlush(entity);
        final Optional<CustomerPasswordEntity> loaded = customerPasswordRepository.findById(CUSTOMER_ID);

        assertThat(saved.getId()).isEqualTo(CUSTOMER_ID);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().getPasswordHash()).isEqualTo(PASSWORD_HASH);
    }
}
