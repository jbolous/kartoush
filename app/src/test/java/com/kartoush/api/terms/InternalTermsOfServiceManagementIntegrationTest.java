package com.kartoush.api.terms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.persistence.repository.TermsOfServiceRepository;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.testsupport.PostgresSpringIntegrationTest;
import com.kartoush.testsupport.SpringIntegrationTest;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringIntegrationTest
@AutoConfigureMockMvc
class InternalTermsOfServiceManagementIntegrationTest extends PostgresSpringIntegrationTest {

    private static final String BASE_URL = "/internal/terms-of-service";
    private static final String INTERNAL_ADMIN_USERNAME = "internal-admin";
    private static final String INTERNAL_ADMIN_PASSWORD = "test-internal-admin-password";
    private static final String CURRENT_VERSION = "2026.04.01";
    private static final String CURRENT_CONTENT = "Current active terms content";
    private static final Instant CURRENT_EFFECTIVE_AT = Instant.parse("2026-04-01T00:00:00Z");
    private static final String DRAFT_VERSION = "2026.06.01";
    private static final String DRAFT_VERSION_WITH_WHITESPACE = "   2026.06.01   ";
    private static final String ACTIVATED_VERSION = "2026.07.01";
    private static final String PROMOTED_VERSION = "2026.08.01";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private TermsOfServiceRepository termsOfServiceRepository;

    @Autowired
    private UlidGenerator ulidGenerator;

    @BeforeEach
    void setUp() {
        deleteIfExists(DRAFT_VERSION);
        deleteIfExists(ACTIVATED_VERSION);
        deleteIfExists(PROMOTED_VERSION);
        deleteIfExists(DRAFT_VERSION_WITH_WHITESPACE.trim());
        ensureCurrentActiveTerms();
    }

    @Test
    void shouldCreateDraftThroughHttp() throws Exception {
        final CreateTermsOfServiceDraftRequest request = new CreateTermsOfServiceDraftRequest(
            DRAFT_VERSION,
            "Draft terms content",
            TermsOfServiceContentType.MARKDOWN
        );

        mockMvc.perform(post(BASE_URL + "/drafts")
                .with(httpBasic(INTERNAL_ADMIN_USERNAME, INTERNAL_ADMIN_PASSWORD))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.version").value(DRAFT_VERSION))
            .andExpect(jsonPath("$.status").value(TermsOfServiceStatus.DRAFT.name()))
            .andExpect(jsonPath("$.id").isNotEmpty());

        final Optional<TermsOfServiceEntity> createdDraft = termsOfServiceRepository.findByVersion(DRAFT_VERSION);
        assertThat(createdDraft).isPresent();
        assertThat(createdDraft.orElseThrow().getStatus()).isEqualTo(TermsOfServiceStatus.DRAFT);
    }

    @Test
    void shouldUpdateDraftThroughHttp() throws Exception {
        final TermsOfServiceEntity draft = termsOfServiceRepository.save(TermsOfServiceEntity.draft(
            ulidGenerator.next(),
            DRAFT_VERSION,
            "Original content",
            TermsOfServiceContentType.PLAIN_TEXT
        ));

        final UpdateTermsOfServiceDraftRequest request = new UpdateTermsOfServiceDraftRequest(
            "Updated content",
            TermsOfServiceContentType.MARKDOWN
        );

        mockMvc.perform(put(BASE_URL + "/drafts/{termsOfServiceId}", draft.getId())
                .with(httpBasic(INTERNAL_ADMIN_USERNAME, INTERNAL_ADMIN_PASSWORD))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Updated content"))
            .andExpect(jsonPath("$.contentType").value(TermsOfServiceContentType.MARKDOWN.name()));

        final TermsOfServiceEntity updatedDraft = termsOfServiceRepository.findById(Objects.requireNonNull(draft.getId())).orElseThrow();
        assertThat(updatedDraft.getContent()).isEqualTo("Updated content");
        assertThat(updatedDraft.getContentType()).isEqualTo(TermsOfServiceContentType.MARKDOWN);
    }

    @Test
    void shouldScheduleAndUnscheduleThroughHttp() throws Exception {
        final TermsOfServiceEntity draft = termsOfServiceRepository.save(TermsOfServiceEntity.draft(
            ulidGenerator.next(),
            DRAFT_VERSION,
            "Draft content",
            TermsOfServiceContentType.PLAIN_TEXT
        ));
        final ScheduleTermsOfServiceRequest scheduleRequest = new ScheduleTermsOfServiceRequest(
            Instant.parse("2026-06-10T00:00:00Z")
        );

        mockMvc.perform(post(BASE_URL + "/{termsOfServiceId}/schedule", draft.getId())
                .with(httpBasic(INTERNAL_ADMIN_USERNAME, INTERNAL_ADMIN_PASSWORD))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(scheduleRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(TermsOfServiceStatus.SCHEDULED.name()));

        mockMvc.perform(post(BASE_URL + "/{termsOfServiceId}/unschedule", draft.getId())
                .with(httpBasic(INTERNAL_ADMIN_USERNAME, INTERNAL_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(TermsOfServiceStatus.DRAFT.name()))
            .andExpect(jsonPath("$.effectiveAt").doesNotExist());

        final TermsOfServiceEntity unscheduledDraft = termsOfServiceRepository.findById(Objects.requireNonNull(draft.getId())).orElseThrow();
        assertThat(unscheduledDraft.getStatus()).isEqualTo(TermsOfServiceStatus.DRAFT);
        assertThat(unscheduledDraft.getEffectiveAt()).isNull();
    }

    @Test
    void shouldActivateDraftThroughHttp() throws Exception {
        final TermsOfServiceEntity draft = termsOfServiceRepository.save(TermsOfServiceEntity.draft(
            ulidGenerator.next(),
            ACTIVATED_VERSION,
            "Activatable content",
            TermsOfServiceContentType.MARKDOWN
        ));

        mockMvc.perform(post(BASE_URL + "/{termsOfServiceId}/activate", draft.getId())
                .with(httpBasic(INTERNAL_ADMIN_USERNAME, INTERNAL_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(ACTIVATED_VERSION))
            .andExpect(jsonPath("$.status").value(TermsOfServiceStatus.ACTIVE.name()));

        final TermsOfServiceEntity activatedTerms = termsOfServiceRepository.findById(Objects.requireNonNull(draft.getId())).orElseThrow();
        final TermsOfServiceEntity previousActiveTerms = termsOfServiceRepository.findByVersion(CURRENT_VERSION).orElseThrow();

        assertThat(activatedTerms.getStatus()).isEqualTo(TermsOfServiceStatus.ACTIVE);
        assertThat(previousActiveTerms.getStatus()).isEqualTo(TermsOfServiceStatus.SUPERSEDED);
        assertThat(previousActiveTerms.getSupersededAt()).isNotNull();
    }

    @Test
    void shouldPromoteDueScheduledTermsThroughHttp() throws Exception {
        final TermsOfServiceEntity dueScheduledTerms = termsOfServiceRepository.save(TermsOfServiceEntity.rehydrate(
            ulidGenerator.next(),
            PROMOTED_VERSION,
            "Promotable content",
            TermsOfServiceContentType.PLAIN_TEXT,
            TermsOfServiceStatus.SCHEDULED,
            Instant.parse("2026-01-01T00:00:00Z"),
            null,
            Instant.parse("2025-12-01T00:00:00Z"),
            Instant.parse("2025-12-01T00:00:00Z")
        ));

        mockMvc.perform(post(BASE_URL + "/promote-due")
                .with(httpBasic(INTERNAL_ADMIN_USERNAME, INTERNAL_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(PROMOTED_VERSION))
            .andExpect(jsonPath("$.status").value(TermsOfServiceStatus.ACTIVE.name()));

        final TermsOfServiceEntity promotedTerms = termsOfServiceRepository.findById(Objects.requireNonNull(dueScheduledTerms.getId())).orElseThrow();
        final TermsOfServiceEntity previousActiveTerms = termsOfServiceRepository.findByVersion(CURRENT_VERSION).orElseThrow();

        assertThat(promotedTerms.getStatus()).isEqualTo(TermsOfServiceStatus.ACTIVE);
        assertThat(previousActiveTerms.getStatus()).isEqualTo(TermsOfServiceStatus.SUPERSEDED);
        assertThat(previousActiveTerms.getSupersededAt()).isNotNull();
    }

    @Test
    void shouldReturnConflictWhenDuplicateVersionSubmittedWithWhitespace() throws Exception {
        final CreateTermsOfServiceDraftRequest request = new CreateTermsOfServiceDraftRequest(
            DRAFT_VERSION_WITH_WHITESPACE,
            "Draft terms content",
            TermsOfServiceContentType.MARKDOWN
        );

        mockMvc.perform(post(BASE_URL + "/drafts")
                .with(httpBasic(INTERNAL_ADMIN_USERNAME, INTERNAL_ADMIN_PASSWORD))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.version").value(DRAFT_VERSION))
            .andExpect(jsonPath("$.status").value(TermsOfServiceStatus.DRAFT.name()))
            .andExpect(jsonPath("$.id").isNotEmpty());

        final Optional<TermsOfServiceEntity> createdDraft = termsOfServiceRepository.findByVersion(DRAFT_VERSION);
        assertThat(createdDraft).isPresent();
        assertThat(createdDraft.orElseThrow().getVersion()).isEqualTo(DRAFT_VERSION);
    }

    @Test
    void shouldReturnConflictWhenEquivalentTrimmedVersionAlreadyExists() throws Exception {
        termsOfServiceRepository.save(TermsOfServiceEntity.draft(
            ulidGenerator.next(),
            DRAFT_VERSION,
            "Existing draft content",
            TermsOfServiceContentType.MARKDOWN
        ));

        final CreateTermsOfServiceDraftRequest request = new CreateTermsOfServiceDraftRequest(
            DRAFT_VERSION_WITH_WHITESPACE,
            "Draft terms content",
            TermsOfServiceContentType.MARKDOWN
        );

        mockMvc.perform(post(BASE_URL + "/drafts")
                .with(httpBasic(INTERNAL_ADMIN_USERNAME, INTERNAL_ADMIN_PASSWORD))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    private void deleteIfExists(final String version) {
        termsOfServiceRepository.findByVersion(version).ifPresent(termsOfServiceRepository::delete);
    }

    private void ensureCurrentActiveTerms() {
        final Optional<TermsOfServiceEntity> currentTerms = termsOfServiceRepository.findByVersion(CURRENT_VERSION);
        if (currentTerms.isPresent() && currentTerms.orElseThrow().getStatus() == TermsOfServiceStatus.ACTIVE) {
            return;
        }

        currentTerms.ifPresent(termsOfServiceRepository::delete);

        termsOfServiceRepository.save(TermsOfServiceEntity.rehydrate(
            ulidGenerator.next(),
            CURRENT_VERSION,
            CURRENT_CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT,
            TermsOfServiceStatus.ACTIVE,
            CURRENT_EFFECTIVE_AT,
            null,
            CURRENT_EFFECTIVE_AT,
            CURRENT_EFFECTIVE_AT
        ));
    }
}
