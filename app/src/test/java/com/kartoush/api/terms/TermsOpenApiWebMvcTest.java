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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/terms-of-service/{version}'].get.responses['404'].content['application/problem+json'].schema['$ref']")
                .value("#/components/schemas/ApiProblemResponse"))
            .andExpect(jsonPath("$.paths['/api/terms-of-service/{version}'].get.parameters[0].description")
                .value("Human-readable Terms of Service version"))
            .andExpect(jsonPath("$.paths['/internal/terms-of-service/drafts'].post.responses['409'].content['application/problem+json'].schema['$ref']")
                .value("#/components/schemas/ApiProblemResponse"))
            .andExpect(jsonPath("$.paths['/internal/terms-of-service/{termsOfServiceId}/schedule'].post.responses['400'].content['application/problem+json'].schema.oneOf[0]['$ref']")
                .value("#/components/schemas/ValidationProblemResponse"))
            .andExpect(jsonPath("$.paths['/internal/terms-of-service/{termsOfServiceId}/schedule'].post.responses['400'].content['application/problem+json'].schema.oneOf[1]['$ref']")
                .value("#/components/schemas/ApiProblemResponse"))
            .andExpect(jsonPath("$.paths['/internal/terms-of-service/{termsOfServiceId}/activate'].post.parameters[0].description")
                .value("Terms of Service ULID identifier"));
    }
}
