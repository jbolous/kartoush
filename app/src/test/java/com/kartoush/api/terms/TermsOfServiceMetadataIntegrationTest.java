package com.kartoush.api.terms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.persistence.repository.TermsOfServiceRepository;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import com.kartoush.platform.ulid.UlidGenerator;
import com.kartoush.testsupport.PostgresSpringIntegrationTest;
import com.kartoush.testsupport.SpringIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringIntegrationTest
@AutoConfigureMockMvc
class TermsOfServiceMetadataIntegrationTest extends PostgresSpringIntegrationTest {

    private static final String BASE_URL = "/api/terms-of-service";
    private static final String CURRENT_VERSION = "2026.04.01";
    private static final String HISTORICAL_VERSION = "2026.03.01";
    private static final String DRAFT_VERSION = "2026.05.01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TermsOfServiceRepository termsOfServiceRepository;

    @Autowired
    private UlidGenerator ulidGenerator;

    @BeforeEach
    void setUp() {
        termsOfServiceRepository.findByVersion(HISTORICAL_VERSION)
            .ifPresent(termsOfServiceRepository::delete);
        termsOfServiceRepository.findByVersion(DRAFT_VERSION)
            .ifPresent(termsOfServiceRepository::delete);

        final TermsOfServiceEntity historicalTerms = TermsOfServiceEntity.rehydrate(
            ulidGenerator.next(),
            HISTORICAL_VERSION,
            "Historical terms content",
            TermsOfServiceContentType.MARKDOWN,
            TermsOfServiceStatus.SUPERSEDED,
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z")
        );
        final TermsOfServiceEntity draftTerms = TermsOfServiceEntity.draft(
            ulidGenerator.next(),
            DRAFT_VERSION,
            "Draft terms content",
            TermsOfServiceContentType.PLAIN_TEXT
        );

        termsOfServiceRepository.save(historicalTerms);
        termsOfServiceRepository.save(draftTerms);
    }

    @Test
    void shouldReturnCurrentTermsOfServiceMetadata() throws Exception {
        mockMvc.perform(get(BASE_URL + "/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(CURRENT_VERSION))
            .andExpect(jsonPath("$.status").value(TermsOfServiceStatus.ACTIVE.name()))
            .andExpect(jsonPath("$.content").isNotEmpty())
            .andExpect(jsonPath("$.contentType").value(TermsOfServiceContentType.PLAIN_TEXT.name()))
            .andExpect(jsonPath("$.effectiveAt").exists());
    }

    @Test
    void shouldReturnHistoricalTermsOfServiceMetadataByVersion() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{version}", HISTORICAL_VERSION))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(HISTORICAL_VERSION))
            .andExpect(jsonPath("$.status").value(TermsOfServiceStatus.SUPERSEDED.name()))
            .andExpect(jsonPath("$.content").value("Historical terms content"))
            .andExpect(jsonPath("$.contentType").value(TermsOfServiceContentType.MARKDOWN.name()))
            .andExpect(jsonPath("$.supersededAt").exists());
    }

    @Test
    void shouldReturnNotFoundForDraftTermsVersion() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{version}", DRAFT_VERSION))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Terms of Service Not Found"));
    }
}
