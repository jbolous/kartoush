package com.kartoush.customer.internal.facade;

import com.kartoush.customer.CustomerTestApplication;
import com.kartoush.customer.exception.CurrentTermsOfServiceNotFoundException;
import com.kartoush.customer.exception.TermsOfServiceVersionNotFoundException;
import com.kartoush.customer.facade.TermsOfServiceFacade;
import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.persistence.repository.TermsOfServiceRepository;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import com.kartoush.platform.ulid.DefaultUlidGenerator;
import com.kartoush.testsupport.IntegrationTest;
import com.kartoush.testsupport.PostgresDataJpaTest;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CustomerTestApplication.class)
@Import({DefaultTermsOfServiceFacade.class, TermsOfServiceMetadataIntegrationTest.SqlInspection.class})
class TermsOfServiceMetadataIntegrationTest extends PostgresDataJpaTest {
    @Autowired private TermsOfServiceFacade facade;
    @Autowired private TermsOfServiceRepository repository;
    @Autowired private Statements statements;

    @BeforeEach
    void setUp() {
        repository.deleteAllInBatch();
        statements.sql.clear();
    }

    @Test
    void shouldSelectOnlyMetadataWhenCurrentValidatorIsRequested() {
        final var terms = draft("2026.05.01");
        terms.activate(Instant.parse("2026-05-01T00:00:00Z"));
        repository.saveAndFlush(terms);
        statements.sql.clear();

        final var metadata = facade.getCurrentTermsOfServiceMetadata();

        assertThat(metadata.version()).isEqualTo(terms.getVersion());
        assertThat(metadata.status()).isEqualTo(TermsOfServiceStatus.ACTIVE);
        assertThat(metadata.effectiveAt()).isEqualTo(terms.getEffectiveAt());
        assertThat(statements.sql).singleElement().satisfies(sql ->
            assertThat(sql).contains("version", "content_type", "status", "effective_at", "superseded_at")
                .doesNotContainPattern("\\bcontent\\b"));
    }

    @Test
    void shouldReflectLifecycleMetadataWhenPublishedTermsAreSuperseded() {
        final var terms = draft("2026.05.01");
        terms.activate(Instant.parse("2026-05-01T00:00:00Z"));
        repository.saveAndFlush(terms);
        terms.supersede(Instant.parse("2026-06-01T00:00:00Z"));
        repository.saveAndFlush(terms);
        statements.sql.clear();

        final var metadata = facade.getTermsOfServiceMetadataByVersion(terms.getVersion());

        assertThat(metadata.status()).isEqualTo(TermsOfServiceStatus.SUPERSEDED);
        assertThat(metadata.supersededAt()).isEqualTo(terms.getSupersededAt());
        assertThat(statements.sql).singleElement().satisfies(sql ->
            assertThat(sql).doesNotContainPattern("\\bcontent\\b"));
    }

    @Test
    void shouldHideUnpublishedVersionsWhenMetadataIsRequested() {
        repository.saveAndFlush(draft("draft"));
        final var scheduled = draft("scheduled");
        scheduled.schedule(Instant.parse("2030-01-01T00:00:00Z"));
        repository.saveAndFlush(scheduled);
        for (final String version : List.of("draft", "scheduled", "missing")) {
            assertThatThrownBy(() -> facade.getTermsOfServiceMetadataByVersion(version))
                .isInstanceOf(TermsOfServiceVersionNotFoundException.class);
        }
        assertThatThrownBy(facade::getCurrentTermsOfServiceMetadata)
            .isInstanceOf(CurrentTermsOfServiceNotFoundException.class);
    }

    private TermsOfServiceEntity draft(final String version) {
        return TermsOfServiceEntity.draft(new DefaultUlidGenerator().next(), version,
            "Large published content", TermsOfServiceContentType.MARKDOWN);
    }

    static class Statements implements StatementInspector {
        final List<String> sql = new ArrayList<>();
        @Override public String inspect(final String statement) {
            sql.add(statement);
            return statement;
        }
    }

    @TestConfiguration
    static class SqlInspection {
        @Bean Statements statements() { return new Statements(); }
        @Bean HibernatePropertiesCustomizer statementInspector(final Statements statements) {
            return properties -> properties.put("hibernate.session_factory.statement_inspector", statements);
        }
    }
}
