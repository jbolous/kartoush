package com.kartoush.api.terms;

import com.kartoush.api.error.ApiExceptionHandler;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.api.error.ErrorCode;
import com.kartoush.config.OpenApiConfiguration;
import com.kartoush.customer.exception.InvalidTermsOfServiceScheduleException;
import com.kartoush.customer.exception.TermsOfServiceVersionNotFoundException;
import com.kartoush.customer.facade.TermsOfServiceFacade;
import com.kartoush.customer.facade.TermsOfServiceManagementFacade;
import com.kartoush.customer.facade.model.TermsOfServiceView;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
    TermsOfServiceController.class,
    InternalTermsOfServiceManagementController.class
})
@AutoConfigureMockMvc(addFilters = false)
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@Import({
    ApiExceptionHandler.class,
    ApiProblemFactory.class,
    OpenApiConfiguration.class
})
@ImportAutoConfiguration({
    SpringDocConfiguration.class,
    SpringDocSpecPropertiesConfiguration.class,
    SpringDocWebMvcConfiguration.class,
    MultipleOpenApiSupportConfiguration.class
})
class TermsApiContractWebMvcTest {

    private static final String API_DOCS_PATH = "/v3/api-docs";

    private static final String VERSION = "2026.04.01";

    private static final String TERMS_ID = "01KQ0INTERNALTERMS000000001";

    private static final String TERMS_BY_VERSION_PATH = "/api/terms-of-service/{version}";

    private static final String SCHEDULE_PATH =
        "/internal/terms-of-service/{termsOfServiceId}/schedule";

    private static final String TERMS_BY_VERSION_RUNTIME_PATH =
        "/api/terms-of-service/" + VERSION;

    private static final String SCHEDULE_RUNTIME_PATH =
        "/internal/terms-of-service/" + TERMS_ID + "/schedule";

    private static final String API_PROBLEM_RESPONSE_REF =
        "#/components/schemas/ApiProblemResponse";

    private static final String TERMS_OF_SERVICE_VIEW_REF =
        "#/components/schemas/TermsOfServiceView";

    private static final String VALIDATION_PROBLEM_RESPONSE_REF =
        "#/components/schemas/ValidationProblemResponse";

    private static final String TERMS_NOT_FOUND_TYPE =
        "urn:kartoush:error:terms-of-service-not-found";

    private static final String TERMS_NOT_FOUND_TITLE =
        "Terms of Service Not Found";

    private static final String VALIDATION_FAILED_TYPE =
        "urn:kartoush:error:validation-failed";

    private static final String VALIDATION_FAILED_TITLE = "Validation Failed";

    private static final String VALIDATION_FAILED_DETAIL =
        "One or more validation errors occurred.";

    private static final String INVALID_SCHEDULE_TYPE =
        "urn:kartoush:error:invalid-terms-of-service-schedule";

    private static final String INVALID_SCHEDULE_TITLE =
        "Invalid Terms of Service Schedule";

    private static final String INVALID_SCHEDULE_DETAIL =
        "Terms of Service can only be scheduled for a future effectiveAt: 2026-04-01T00:00:00Z";

    private static final String SCHEDULE_BAD_REQUEST_SCHEMA_PATH =
        problemSchemaPath(SCHEDULE_PATH, "post", HttpStatus.BAD_REQUEST.value());

    private static final String SCHEDULE_BAD_REQUEST_VALIDATION_REF_PATH =
        SCHEDULE_BAD_REQUEST_SCHEMA_PATH + ".oneOf[0]['$ref']";

    private static final String SCHEDULE_BAD_REQUEST_PROBLEM_REF_PATH =
        SCHEDULE_BAD_REQUEST_SCHEMA_PATH + ".oneOf[1]['$ref']";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TermsOfServiceFacade termsOfServiceFacade;

    @MockitoBean
    private TermsOfServiceManagementFacade termsOfServiceManagementFacade;

    @Test
    void shouldMatchDocumentedTermsSuccessResponse() throws Exception {
        when(termsOfServiceFacade.getTermsOfServiceByVersion(VERSION))
            .thenReturn(termsOfServiceView());

        mockMvc.perform(get(API_DOCS_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath(schemaRefPath(
                TERMS_BY_VERSION_PATH,
                "get",
                HttpStatus.OK.value()
            )).value(TERMS_OF_SERVICE_VIEW_REF));

        mockMvc.perform(get(TERMS_BY_VERSION_PATH, VERSION))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$.version").value(VERSION))
            .andExpect(jsonPath("$.content").value("Terms content"))
            .andExpect(jsonPath("$.contentType").value(TermsOfServiceContentType.PLAIN_TEXT.name()))
            .andExpect(jsonPath("$.status").value(TermsOfServiceStatus.ACTIVE.name()))
            .andExpect(jsonPath("$.effectiveAt").value("2026-04-01T00:00:00Z"));
    }

    @Test
    void shouldMatchDocumentedApiProblemForMissingTermsVersion() throws Exception {
        when(termsOfServiceFacade.getTermsOfServiceByVersion(VERSION))
            .thenThrow(new TermsOfServiceVersionNotFoundException(VERSION));

        mockMvc.perform(get(API_DOCS_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath(problemSchemaRefPath(
                TERMS_BY_VERSION_PATH,
                "get",
                HttpStatus.NOT_FOUND.value()
            )).value(API_PROBLEM_RESPONSE_REF));

        mockMvc.perform(get(TERMS_BY_VERSION_PATH, VERSION))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value(TERMS_NOT_FOUND_TYPE))
            .andExpect(jsonPath("$.title").value(TERMS_NOT_FOUND_TITLE))
            .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
            .andExpect(jsonPath("$.detail").value("Terms of Service not found for version: " + VERSION))
            .andExpect(jsonPath("$.instance").value(TERMS_BY_VERSION_RUNTIME_PATH))
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.TERMS_OF_SERVICE_NOT_FOUND.name()))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldMatchDocumentedValidationProblemForScheduleRequest() throws Exception {
        mockMvc.perform(get(API_DOCS_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath(SCHEDULE_BAD_REQUEST_VALIDATION_REF_PATH)
                .value(VALIDATION_PROBLEM_RESPONSE_REF));

        mockMvc.perform(post(SCHEDULE_PATH, TERMS_ID)
                .contentType(APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value(VALIDATION_FAILED_TYPE))
            .andExpect(jsonPath("$.title").value(VALIDATION_FAILED_TITLE))
            .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.detail").value(VALIDATION_FAILED_DETAIL))
            .andExpect(jsonPath("$.instance").value(SCHEDULE_RUNTIME_PATH))
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_FAILED.name()))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.errors[0].field").value("effectiveAt"))
            .andExpect(jsonPath("$.errors[0].message").value("effectiveAt is required"))
            .andExpect(jsonPath("$.errors[0].code").value("NotNull"));
    }

    @Test
    void shouldMatchDocumentedApiProblemForInvalidSchedule() throws Exception {
        when(termsOfServiceManagementFacade.schedule(TERMS_ID, Instant.parse("2026-04-01T00:00:00Z")))
            .thenThrow(new InvalidTermsOfServiceScheduleException(Instant.parse("2026-04-01T00:00:00Z")));

        mockMvc.perform(get(API_DOCS_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath(SCHEDULE_BAD_REQUEST_PROBLEM_REF_PATH)
                .value(API_PROBLEM_RESPONSE_REF));

        mockMvc.perform(post(SCHEDULE_PATH, TERMS_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "effectiveAt": "2026-04-01T00:00:00Z"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value(INVALID_SCHEDULE_TYPE))
            .andExpect(jsonPath("$.title").value(INVALID_SCHEDULE_TITLE))
            .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.detail").value(INVALID_SCHEDULE_DETAIL))
            .andExpect(jsonPath("$.instance").value(SCHEDULE_RUNTIME_PATH))
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_TERMS_OF_SERVICE_SCHEDULE.name()))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    private static String problemSchemaRefPath(
        final String endpoint,
        final String method,
        final int statusCode
    ) {
        return problemSchemaPath(endpoint, method, statusCode) + "['$ref']";
    }

    private static String schemaRefPath(
        final String endpoint,
        final String method,
        final int statusCode
    ) {
        return operationPath(
            endpoint,
            method,
            "responses['" + statusCode + "']"
                + ".content['application/json']"
                + ".schema['$ref']"
        );
    }

    private static String problemSchemaPath(
        final String endpoint,
        final String method,
        final int statusCode
    ) {
        return operationPath(
            endpoint,
            method,
            "responses['" + statusCode + "']"
                + ".content['" + APPLICATION_PROBLEM_JSON_VALUE + "']"
                + ".schema"
        );
    }

    private static String operationPath(
        final String endpoint,
        final String method,
        final String suffix
    ) {
        return "$.paths['" + endpoint + "']." + method + "." + suffix;
    }

    private static TermsOfServiceView termsOfServiceView() {
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
