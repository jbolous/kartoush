package com.kartoush.api.terms;

import com.kartoush.api.error.ApiExceptionHandler;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.config.ApiResponseCacheConfiguration;
import com.kartoush.customer.exception.TermsOfServiceVersionNotFoundException;
import com.kartoush.customer.facade.TermsOfServiceFacade;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TermsResponseCachingTest {
    private final TermsOfServiceFacade facade = mock(TermsOfServiceFacade.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final var registration = new ApiResponseCacheConfiguration().termsResponseEtagFilter();
        mockMvc = MockMvcBuilders.standaloneSetup(new TermsOfServiceController(facade))
            .setControllerAdvice(new ApiExceptionHandler(new ApiProblemFactory()))
            .addFilter(registration.getFilter(), registration.getUrlPatterns().toArray(String[]::new))
            .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"current", "2026.04.01"})
    void shouldCacheSuccessfulResponsesAndReturnNotModifiedWhenUnchanged(final String suffix) throws Exception {
        final TermsOfServiceView view = terms("Original content");
        if (suffix.equals("current")) {
            when(facade.getCurrentTermsOfService()).thenReturn(view);
        } else {
            when(facade.getTermsOfServiceByVersion(suffix)).thenReturn(view);
        }
        final String path = "/api/terms-of-service/" + suffix;

        final String etag = mockMvc.perform(get(path))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=60, must-revalidate, public"))
            .andExpect(header().exists(HttpHeaders.ETAG))
            .andReturn().getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(get(path).header(HttpHeaders.IF_NONE_MATCH, etag))
            .andExpect(status().isNotModified())
            .andExpect(header().string(HttpHeaders.ETAG, etag))
            .andExpect(content().string(""));
    }

    @Test
    void shouldReturnNewEtagWhenTermsChange() throws Exception {
        when(facade.getCurrentTermsOfService()).thenReturn(terms("Original"), terms("Updated"));
        final String path = "/api/terms-of-service/current";
        final String originalEtag = mockMvc.perform(get(path)).andReturn().getResponse().getHeader(HttpHeaders.ETAG);

        final String updatedEtag = mockMvc.perform(get(path).header(HttpHeaders.IF_NONE_MATCH, originalEtag))
            .andExpect(status().isOk())
            .andReturn().getResponse().getHeader(HttpHeaders.ETAG);

        assertThat(updatedEtag).isNotNull().isNotEqualTo(originalEtag);
    }

    @Test
    void shouldNotCacheMissingTerms() throws Exception {
        when(facade.getTermsOfServiceByVersion("missing"))
            .thenThrow(new TermsOfServiceVersionNotFoundException("missing"));

        mockMvc.perform(get("/api/terms-of-service/missing"))
            .andExpect(status().isNotFound())
            .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL))
            .andExpect(header().doesNotExist(HttpHeaders.ETAG));
    }

    private TermsOfServiceView terms(final String content) {
        return new TermsOfServiceView("2026.04.01", content, TermsOfServiceContentType.PLAIN_TEXT,
            TermsOfServiceStatus.ACTIVE, null, null);
    }
}
