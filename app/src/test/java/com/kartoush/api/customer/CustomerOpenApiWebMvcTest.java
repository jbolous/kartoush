package com.kartoush.api.customer;

import com.kartoush.api.error.ApiExceptionHandler;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.config.OpenApiConfiguration;
import com.kartoush.customer.facade.CustomerFacade;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
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
class CustomerOpenApiWebMvcTest {

    private static final String API_DOCS_PATH = "/v3/api-docs";

    private static final String CUSTOMER_PATH = "/api/customers";

    private static final String CUSTOMER_BY_ID_PATH = CUSTOMER_PATH + "/{customerId}";

    private static final String CUSTOMER_ACTIVATION_PATH = CUSTOMER_BY_ID_PATH + "/activation";

    private static final String CUSTOMER_INITIAL_PASSWORD_PATH = CUSTOMER_BY_ID_PATH + "/initial-password";

    private static final String CUSTOMER_BY_ID_PARAMETER_DESCRIPTION_PATH =
        operationPath(CUSTOMER_BY_ID_PATH, "get", "parameters[0].description");

    private static final String API_PROBLEM_RESPONSE_REF = "#/components/schemas/ApiProblemResponse";

    private static final String VALIDATION_PROBLEM_RESPONSE_REF = "#/components/schemas/ValidationProblemResponse";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerFacade customerFacade;

    @MockitoBean
    private ApiProblemFactory apiProblemFactory;

    @Test
    void shouldExposeCustomerOpenApiDocumentation() throws Exception {
        mockMvc.perform(get(API_DOCS_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("Kartoush API"))
            .andExpect(jsonPath("$.paths['/api/customers'].get").doesNotExist())
            .andExpect(jsonPath(operationPath(CUSTOMER_PATH, "post", "summary")).value("Create a customer"))
            .andExpect(jsonPath(problemSchemaRefPath(
                CUSTOMER_PATH,
                "post",
                HttpStatus.BAD_REQUEST.value()
            ))
                .value(VALIDATION_PROBLEM_RESPONSE_REF))
            .andExpect(jsonPath(problemSchemaRefPath(
                CUSTOMER_BY_ID_PATH,
                "put",
                HttpStatus.CONFLICT.value()
            ))
                .value(API_PROBLEM_RESPONSE_REF))
            .andExpect(jsonPath(operationPath(CUSTOMER_ACTIVATION_PATH, "post", "summary"))
                .value("Activate a customer"))
            .andExpect(jsonPath(operationPath(CUSTOMER_INITIAL_PASSWORD_PATH, "post", "summary"))
                .value("Set initial customer password"))
            .andExpect(jsonPath(problemSchemaRefPath(
                CUSTOMER_INITIAL_PASSWORD_PATH,
                "post",
                HttpStatus.NOT_FOUND.value()
            ))
                .value(API_PROBLEM_RESPONSE_REF))
            .andExpect(jsonPath(CUSTOMER_BY_ID_PARAMETER_DESCRIPTION_PATH)
                .value("Customer ULID identifier"))
            .andExpect(jsonPath("$.components.schemas.CustomerActivationView.properties.passwordSetupToken.type")
                .value("string"))
            .andExpect(jsonPath("$.components.schemas.ValidationProblemResponse.properties.errors.type").value("array"))
            .andExpect(jsonPath("$.components.schemas.CustomerView.properties.status.description")
                .value("Customer lifecycle status"));
    }

    private static String problemSchemaRefPath(
        final String endpoint,
        final String method,
        final int statusCode
    ) {
        return operationPath(
            endpoint,
            method,
            "responses['" + statusCode + "']"
                + ".content['" + APPLICATION_PROBLEM_JSON_VALUE + "']"
                + ".schema['$ref']"
        );
    }

    private static String operationPath(
        final String endpoint,
        final String method,
        final String suffix
    ) {
        return "$.paths['" + endpoint + "']." + method + "." + suffix;
    }
}
