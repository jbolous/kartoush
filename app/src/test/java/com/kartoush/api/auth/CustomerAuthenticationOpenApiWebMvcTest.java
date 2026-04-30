package com.kartoush.api.auth;

import com.kartoush.api.error.ApiExceptionHandler;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.config.OpenApiConfiguration;
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

import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerAuthenticationController.class)
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
class CustomerAuthenticationOpenApiWebMvcTest {

    private static final String API_DOCS_PATH = "/v3/api-docs";
    private static final String SIGN_IN_PATH = "/api/auth/sign-in";
    private static final String API_PROBLEM_RESPONSE_REF = "#/components/schemas/ApiProblemResponse";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerAuthenticationApplicationService customerAuthenticationApplicationService;

    @MockitoBean
    private ApiProblemFactory apiProblemFactory;

    @Test
    void shouldExposeCustomerAuthenticationOpenApiDocumentation() throws Exception {
        mockMvc.perform(get(API_DOCS_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath(operationPath(SIGN_IN_PATH, "post", "summary"))
                .value("Sign in a customer"))
            .andExpect(jsonPath(problemSchemaRefPath(SIGN_IN_PATH, "post", HttpStatus.UNAUTHORIZED.value()))
                .value(API_PROBLEM_RESPONSE_REF))
            .andExpect(jsonPath("$.components.schemas.CustomerSignInView.properties.accessToken.type")
                .value("string"))
            .andExpect(jsonPath("$.components.schemas.CustomerSignInView.properties.tokenType.type")
                .value("string"));
    }

    private static String problemSchemaRefPath(final String endpoint, final String method, final int statusCode) {
        return operationPath(
            endpoint,
            method,
            "responses['" + statusCode + "']"
                + ".content['" + APPLICATION_PROBLEM_JSON_VALUE + "']"
                + ".schema['$ref']"
        );
    }

    private static String operationPath(final String endpoint, final String method, final String suffix) {
        return "$.paths['" + endpoint + "']." + method + "." + suffix;
    }
}
