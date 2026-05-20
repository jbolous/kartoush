package com.kartoush.api.terms;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.api.error.ErrorCode;
import com.kartoush.customer.exception.TermsOfServiceVersionNotFoundException;
import com.kartoush.customer.facade.TermsOfServiceFacade;
import com.kartoush.customer.facade.TermsOfServiceManagementFacade;
import com.kartoush.customer.facade.model.TermsOfServiceView;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TermsOfServiceController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
    SpringDocConfiguration.class,
    SpringDocSpecPropertiesConfiguration.class,
    SpringDocWebMvcConfiguration.class,
    MultipleOpenApiSupportConfiguration.class
})
class TermsOfServiceControllerWebMvcTest {

    private static final String BASE_URL = "/api/terms-of-service";
    private static final String VERSION = "2026.04.01";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiProblemFactory apiProblemFactory;

    @MockitoBean
    private TermsOfServiceFacade termsOfServiceFacade;

    @MockitoBean
    private TermsOfServiceManagementFacade termsOfServiceManagementFacade;

    @Test
    void shouldGetCurrentTermsOfService() throws Exception {
        when(termsOfServiceFacade.getCurrentTermsOfService()).thenReturn(termsView());

        mockMvc.perform(get(BASE_URL + "/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(VERSION))
            .andExpect(jsonPath("$.status").value(TermsOfServiceStatus.ACTIVE.name()));

        verify(termsOfServiceFacade).getCurrentTermsOfService();
    }

    @Test
    void shouldGetTermsOfServiceByVersion() throws Exception {
        when(termsOfServiceFacade.getTermsOfServiceByVersion(VERSION)).thenReturn(termsView());

        mockMvc.perform(get(BASE_URL + "/{version}", VERSION))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(VERSION))
            .andExpect(jsonPath("$.contentType").value(TermsOfServiceContentType.PLAIN_TEXT.name()));

        verify(termsOfServiceFacade).getTermsOfServiceByVersion(VERSION);
    }

    @Test
    void shouldReturnNotFoundForMissingTermsVersion() throws Exception {
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            "Terms of Service not found for version: " + VERSION
        );
        problemDetail.setTitle("Terms of Service Not Found");
        problemDetail.setProperty("errorCode", ErrorCode.TERMS_OF_SERVICE_NOT_FOUND.name());

        when(termsOfServiceFacade.getTermsOfServiceByVersion(VERSION))
            .thenThrow(new TermsOfServiceVersionNotFoundException(VERSION));
        when(apiProblemFactory.create(
            any(),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(problemDetail);

        mockMvc.perform(get(BASE_URL + "/{version}", VERSION))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Terms of Service Not Found"))
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.TERMS_OF_SERVICE_NOT_FOUND.name()));
    }

    private TermsOfServiceView termsView() {
        return new TermsOfServiceView(
            VERSION,
            "Terms content",
            TermsOfServiceContentType.PLAIN_TEXT,
            TermsOfServiceStatus.ACTIVE,
            Instant.parse("2026-04-01T00:00:00Z"),
            null
        );
    }
}
