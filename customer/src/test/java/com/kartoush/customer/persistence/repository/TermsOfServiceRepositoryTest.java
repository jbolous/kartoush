package com.kartoush.customer.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kartoush.customer.CustomerTestApplication;
import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import com.kartoush.platform.ulid.DefaultUlidGenerator;
import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.testsupport.IntegrationTest;
import com.kartoush.testsupport.PostgresDataJpaTest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

@IntegrationTest
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CustomerTestApplication.class)
class TermsOfServiceRepositoryTest extends PostgresDataJpaTest {

    private static final String VERSION = "2026-05";
    private static final String CONTENT = "Terms content";
    private static final Instant EFFECTIVE_AT = Instant.parse("2026-05-01T00:00:00Z");

    private final UlidGenerator ulidGenerator = new DefaultUlidGenerator();

    @Autowired
    private TermsOfServiceRepository termsOfServiceRepository;

    @BeforeEach
    void setUp() {
        termsOfServiceRepository.deleteAll();
    }

    @Test
    void shouldPersistAndLoadTermsOfServiceByVersion() {
        // given
        final TermsOfServiceEntity terms = TermsOfServiceEntity.draft(
            ulidGenerator.next(),
            VERSION,
            CONTENT,
            TermsOfServiceContentType.MARKDOWN
        );
        terms.activate(EFFECTIVE_AT);

        // when
        termsOfServiceRepository.saveAndFlush(terms);
        final Optional<TermsOfServiceEntity> retrieved = termsOfServiceRepository.findByVersion(VERSION);

        // then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getVersion()).isEqualTo(VERSION);
        assertThat(retrieved.get().getContent()).isEqualTo(CONTENT);
        assertThat(retrieved.get().getContentType()).isEqualTo(TermsOfServiceContentType.MARKDOWN);
        assertThat(retrieved.get().getStatus()).isEqualTo(TermsOfServiceStatus.ACTIVE);
        assertThat(retrieved.get().getEffectiveAt()).isEqualTo(EFFECTIVE_AT);
    }

    @Test
    void shouldFindCurrentActiveTermsOfService() {
        // given
        final TermsOfServiceEntity terms = TermsOfServiceEntity.draft(
            ulidGenerator.next(),
            "2026-06",
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT
        );
        terms.activate(EFFECTIVE_AT);
        termsOfServiceRepository.saveAndFlush(terms);

        // when
        final Optional<TermsOfServiceEntity> retrieved =
            termsOfServiceRepository.findByStatus(TermsOfServiceStatus.ACTIVE);

        // then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getStatus()).isEqualTo(TermsOfServiceStatus.ACTIVE);
    }
}
