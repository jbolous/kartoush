package com.kartoush.customer.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TermsOfServiceEntityTest {

    private static final String TERMS_ID = "01JSVAMTQW65M2Q8B8M4RH6P3M";
    private static final String VERSION = "2026.04.01";
    private static final String CONTENT = "Initial Terms content";
    private static final String UPDATED_CONTENT = "Updated Terms content";
    private static final Instant EFFECTIVE_AT = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant SUPERSEDED_AT = Instant.parse("2026-06-01T00:00:00Z");

    @Test
    void shouldCreateDraftTermsOfService() {
        // given / when
        final TermsOfServiceEntity terms = TermsOfServiceEntity.draft(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.MARKDOWN
        );

        // then
        assertThat(terms.getId()).isEqualTo(TERMS_ID);
        assertThat(terms.getVersion()).isEqualTo(VERSION);
        assertThat(terms.getContent()).isEqualTo(CONTENT);
        assertThat(terms.getContentType()).isEqualTo(TermsOfServiceContentType.MARKDOWN);
        assertThat(terms.getStatus()).isEqualTo(TermsOfServiceStatus.DRAFT);
        assertThat(terms.getEffectiveAt()).isNull();
        assertThat(terms.getSupersededAt()).isNull();
    }

    @Test
    void shouldAllowUpdatingContentWhileDraft() {
        // given
        final TermsOfServiceEntity terms = TermsOfServiceEntity.draft(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT
        );

        // when
        terms.updateDraftContent(UPDATED_CONTENT, TermsOfServiceContentType.HTML);

        // then
        assertThat(terms.getContent()).isEqualTo(UPDATED_CONTENT);
        assertThat(terms.getContentType()).isEqualTo(TermsOfServiceContentType.HTML);
    }

    @Test
    void shouldScheduleDraftTermsOfService() {
        // given
        final TermsOfServiceEntity terms = TermsOfServiceEntity.draft(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT
        );

        // when
        terms.schedule(EFFECTIVE_AT);

        // then
        assertThat(terms.getStatus()).isEqualTo(TermsOfServiceStatus.SCHEDULED);
        assertThat(terms.getEffectiveAt()).isEqualTo(EFFECTIVE_AT);
    }

    @Test
    void shouldActivateScheduledTermsOfService() {
        // given
        final TermsOfServiceEntity terms = TermsOfServiceEntity.draft(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT
        );
        terms.schedule(EFFECTIVE_AT);

        // when
        terms.activate(EFFECTIVE_AT);

        // then
        assertThat(terms.getStatus()).isEqualTo(TermsOfServiceStatus.ACTIVE);
        assertThat(terms.getEffectiveAt()).isEqualTo(EFFECTIVE_AT);
        assertThat(terms.getSupersededAt()).isNull();
    }

    @Test
    void shouldSupersedeActiveTermsOfService() {
        // given
        final TermsOfServiceEntity terms = TermsOfServiceEntity.draft(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT
        );
        terms.activate(EFFECTIVE_AT);

        // when
        terms.supersede(SUPERSEDED_AT);

        // then
        assertThat(terms.getStatus()).isEqualTo(TermsOfServiceStatus.SUPERSEDED);
        assertThat(terms.getSupersededAt()).isEqualTo(SUPERSEDED_AT);
    }

    @Test
    void shouldRejectContentChangesAfterDraft() {
        // given
        final TermsOfServiceEntity terms = TermsOfServiceEntity.draft(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT
        );
        terms.activate(EFFECTIVE_AT);

        // when / then
        assertThatThrownBy(() -> terms.updateDraftContent(UPDATED_CONTENT, TermsOfServiceContentType.HTML))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Terms of Service content can only be modified while in DRAFT");
    }
}
