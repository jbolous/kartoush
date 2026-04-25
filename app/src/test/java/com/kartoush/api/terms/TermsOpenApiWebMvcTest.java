package com.kartoush.api.terms;

import com.kartoush.api.error.ApiExceptionHandler;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.config.OpenApiConfiguration;
import com.kartoush.customer.facade.TermsOfServiceFacade;
import com.kartoush.customer.facade.TermsOfServiceManagementFacade;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
    TermsOfServiceController.class,
    InternalTermsOfServiceManagementController.class
})
@AutoConfigureMockMvc
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@Import({
    ApiExceptionHandler.class,
    OpenApiConfiguration.class
})
@ImportAutoConfiguration({
    SpringDocConfiguration.class,
    SpringDocSpecPropertiesConfiguration.class,
    SpringDocWebMvcConfiguration.class,
    MultipleOpenApiSupportConfiguration.class
})
class TermsOpenApiWebMvcTest {

    private static final String API_DOCS_PATH = "/v3/api-docs";

    private static final String PUBLIC_TERMS_PATH = "/api/terms-of-service";

    private static final String TERMS_BY_VERSION_PATH = PUBLIC_TERMS_PATH + "/{version}";

    private static final String INTERNAL_TERMS_PATH = "/internal/terms-of-service";

    private static final String INTERNAL_DRAFTS_PATH = INTERNAL_TERMS_PATH + "/drafts";

    private static final String INTERNAL_SCHEDULE_PATH =
        INTERNAL_TERMS_PATH + "/{termsOfServiceId}/schedule";

    private static final String INTERNAL_ACTIVATE_PATH =
        INTERNAL_TERMS_PATH + "/{termsOfServiceId}/activate";

    private static final String TERMS_BY_VERSION_PARAMETER_DESCRIPTION_PATH =
        operationPath(TERMS_BY_VERSION_PATH, "get", "parameters[0].description");

    private static final String INTERNAL_ACTIVATE_PARAMETER_DESCRIPTION_PATH =
        operationPath(INTERNAL_ACTIVATE_PATH, "post", "parameters[0].description");

    private static final String SCHEDULE_BAD_REQUEST_SCHEMA_PATH =
        problemSchemaPath(
            INTERNAL_SCHEDULE_PATH,
            "post",
            HttpStatus.BAD_REQUEST.value()
        );

    private static final String SCHEDULE_BAD_REQUEST_VALIDATION_REF_PATH =
        SCHEDULE_BAD_REQUEST_SCHEMA_PATH + ".oneOf[0]['$ref']";

    private static final String SCHEDULE_BAD_REQUEST_PROBLEM_REF_PATH =
        SCHEDULE_BAD_REQUEST_SCHEMA_PATH + ".oneOf[1]['$ref']";

    private static final String API_PROBLEM_RESPONSE_REF = "#/components/schemas/ApiProblemResponse";

    private static final String VALIDATION_PROBLEM_RESPONSE_REF = "#/components/schemas/ValidationProblemResponse";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TermsOfServiceFacade termsOfServiceFacade;

    @MockitoBean
    private TermsOfServiceManagementFacade termsOfServiceManagementFacade;

    @MockitoBean
    private ApiProblemFactory apiProblemFactory;

    @Test
    void shouldExposeTermsProblemDetailDocumentation() throws Exception {
        mockMvc.perform(get(API_DOCS_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath(problemSchemaRefPath(
                TERMS_BY_VERSION_PATH,
                "get",
                HttpStatus.NOT_FOUND.value()
            ))
                .value(API_PROBLEM_RESPONSE_REF))
            .andExpect(jsonPath(TERMS_BY_VERSION_PARAMETER_DESCRIPTION_PATH)
                .value("Human-readable Terms of Service version"))
            .andExpect(jsonPath(problemSchemaRefPath(
                INTERNAL_DRAFTS_PATH,
                "post",
                HttpStatus.CONFLICT.value()
            ))
                .value(API_PROBLEM_RESPONSE_REF))
            .andExpect(jsonPath(SCHEDULE_BAD_REQUEST_VALIDATION_REF_PATH)
                .value(VALIDATION_PROBLEM_RESPONSE_REF))
            .andExpect(jsonPath(SCHEDULE_BAD_REQUEST_PROBLEM_REF_PATH)
                .value(API_PROBLEM_RESPONSE_REF))
            .andExpect(jsonPath(INTERNAL_ACTIVATE_PARAMETER_DESCRIPTION_PATH)
                .value("Terms of Service ULID identifier"));
    }

    private static String operationPath(
        final String endpoint,
        final String method,
        final String suffix
    ) {
        return "$.paths['" + endpoint + "']." + method + "." + suffix;
    }

    private static String problemSchemaRefPath(
        final String endpoint,
        final String method,
        final int statusCode
    ) {
        return problemSchemaRefPath(problemSchemaPath(endpoint, method, statusCode));
    }

    private static String problemSchemaPath(
        final String endpoint,
        final String method,
        final int statusCode
    ) {
        return "$.paths['" + endpoint + "']." + method
            + ".responses['" + statusCode + "']"
            + ".content['" + APPLICATION_PROBLEM_JSON_VALUE + "']"
            + ".schema";
    }

    private static String problemSchemaRefPath(final String problemSchemaPath) {
        return problemSchemaPath + "['$ref']";
    }
}
