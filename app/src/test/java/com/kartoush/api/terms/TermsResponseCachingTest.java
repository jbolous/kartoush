package com.kartoush.api.terms;

import com.kartoush.api.error.ApiExceptionHandler;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.customer.exception.TermsOfServiceVersionNotFoundException;
import com.kartoush.customer.facade.TermsOfServiceFacade;
import com.kartoush.customer.facade.model.TermsOfServiceMetadataView;
import com.kartoush.customer.facade.model.TermsOfServiceView;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TermsResponseCachingTest {
    private static final String BASE = "/api/terms-of-service/";
    private final TermsOfServiceFacade facade = mock(TermsOfServiceFacade.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TermsOfServiceController(facade))
            .setControllerAdvice(new ApiExceptionHandler(new ApiProblemFactory())).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"current", "2026.04.01"})
    void shouldSkipContentLookupWhenValidatorMatches(final String suffix) throws Exception {
        stub(terms("2026.04.01", TermsOfServiceStatus.ACTIVE, null));
        final String etag = mockMvc.perform(get(BASE + suffix)).andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=60, must-revalidate, public"))
            .andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(etag).startsWith("W/\"terms-json-v1-").endsWith("\"");
        clearInvocations(facade);
        mockMvc.perform(get(BASE + suffix).header(HttpHeaders.IF_NONE_MATCH, etag))
            .andExpect(status().isNotModified()).andExpect(content().string(""))
            .andExpect(header().string(HttpHeaders.ETAG, etag))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=60, must-revalidate, public"));
        verify(facade, never()).getCurrentTermsOfService();
        verify(facade, never()).getTermsOfServiceByVersion(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"wildcard", "strong", "list", "head"})
    void shouldHonorHttpConditionalSemanticsWhenContentExists(final String kind) throws Exception {
        stub(terms("2026.04.01", TermsOfServiceStatus.ACTIVE, null));
        final String etag = mockMvc.perform(get(BASE + "current")).andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        clearInvocations(facade);
        final String condition = switch (kind) {
            case "wildcard" -> "*";
            case "strong" -> etag.substring(2);
            case "list" -> "\"different\", " + etag;
            default -> etag;
        };
        final var request = kind.equals("head") ? head(BASE + "current") : get(BASE + "current");
        mockMvc.perform(request.header(HttpHeaders.IF_NONE_MATCH, condition))
            .andExpect(status().isNotModified()).andExpect(content().string(""));
        verify(facade, never()).getCurrentTermsOfService();
    }

    @Test
    void shouldReturnNewValidatorWhenCurrentVersionChanges() throws Exception {
        stub(terms("2026.04.01", TermsOfServiceStatus.ACTIVE, null));
        final String etag = mockMvc.perform(get(BASE + "current")).andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        stub(terms("2026.05.01", TermsOfServiceStatus.ACTIVE, null));
        final String changed = mockMvc.perform(get(BASE + "current").header(HttpHeaders.IF_NONE_MATCH, etag))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value("2026.05.01"))
            .andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(changed).isNotEqualTo(etag);
    }

    @Test
    void shouldReturnNewValidatorWhenPublishedVersionIsSuperseded() throws Exception {
        stub(terms("2026.04.01", TermsOfServiceStatus.ACTIVE, null));
        final String etag = mockMvc.perform(get(BASE + "2026.04.01")).andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        stub(terms("2026.04.01", TermsOfServiceStatus.SUPERSEDED, Instant.parse("2026-05-01T00:00:00Z")));
        final String changed = mockMvc.perform(get(BASE + "2026.04.01").header(HttpHeaders.IF_NONE_MATCH, etag))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUPERSEDED"))
            .andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(changed).isNotEqualTo(etag);
    }

    @Test
    void shouldTagLoadedBodyWhenPublicationRacesMetadataLookup() throws Exception {
        stub(terms("2026.04.01", TermsOfServiceStatus.ACTIVE, null));
        final String originalTag = mockMvc.perform(get(BASE + "current")).andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        when(facade.getCurrentTermsOfService()).thenReturn(terms("2026.05.01", TermsOfServiceStatus.ACTIVE, null));
        final String racedTag = mockMvc.perform(get(BASE + "current").header(HttpHeaders.IF_NONE_MATCH, "\"unrelated\""))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value("2026.05.01"))
            .andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        final String bodyTag = mockMvc.perform(get(BASE + "current")).andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(racedTag).isNotEqualTo(originalTag).isEqualTo(bodyTag);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unconditional", "conditional"})
    void shouldNotCacheMissingTermsWhenVersionIsNotPublished(final String kind) throws Exception {
        when(facade.getTermsOfServiceByVersion("missing"))
            .thenThrow(new TermsOfServiceVersionNotFoundException("missing"));
        when(facade.getTermsOfServiceMetadataByVersion("missing"))
            .thenThrow(new TermsOfServiceVersionNotFoundException("missing"));
        final var request = get(BASE + "missing");
        if (kind.equals("conditional")) { request.header(HttpHeaders.IF_NONE_MATCH, "*"); }
        mockMvc.perform(request).andExpect(status().isNotFound())
            .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL))
            .andExpect(header().doesNotExist(HttpHeaders.ETAG));
    }

    private void stub(final TermsOfServiceView view) {
        when(facade.getCurrentTermsOfService()).thenReturn(view);
        when(facade.getCurrentTermsOfServiceMetadata()).thenReturn(TermsOfServiceMetadataView.from(view));
        when(facade.getTermsOfServiceByVersion(view.version())).thenReturn(view);
        when(facade.getTermsOfServiceMetadataByVersion(view.version())).thenReturn(TermsOfServiceMetadataView.from(view));
    }

    private TermsOfServiceView terms(final String version, final TermsOfServiceStatus status, final Instant supersededAt) {
        return new TermsOfServiceView(version, "Content for " + version, TermsOfServiceContentType.PLAIN_TEXT,
            status, Instant.parse("2026-04-01T00:00:00Z"), supersededAt);
    }
}
