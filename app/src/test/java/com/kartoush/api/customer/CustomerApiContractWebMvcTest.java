package com.kartoush.api.customer;

import com.kartoush.api.error.ApiExceptionHandler;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.api.error.ErrorCode;
import com.kartoush.config.OpenApiConfiguration;
import com.kartoush.customer.exception.CustomerNotFoundException;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.platform.types.CustomerStatus;
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

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc
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
class CustomerApiContractWebMvcTest {

    private static final String API_DOCS_PATH = "/v3/api-docs";

    private static final String CUSTOMER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    private static final String FIRST_NAME = "Jack";

    private static final String LAST_NAME = "Kartoush";

    private static final String EMAIL = "jack@kartoush.test";

    private static final String PHONE_NUMBER = "+13125551234";

    private static final String CUSTOMER_PATH = "/api/customers";

    private static final String CUSTOMER_BY_ID_PATH = CUSTOMER_PATH + "/{customerId}";

    private static final String CUSTOMER_ACTIVATION_PATH =
        CUSTOMER_BY_ID_PATH + "/activation";

    private static final String CUSTOMER_BY_ID_RUNTIME_PATH =
        "/api/customers/" + CUSTOMER_ID;

    private static final String CUSTOMER_ACTIVATION_RUNTIME_PATH =
        CUSTOMER_BY_ID_RUNTIME_PATH + "/activation";

    private static final String API_PROBLEM_RESPONSE_REF =
        "#/components/schemas/ApiProblemResponse";

    private static final String CUSTOMER_VIEW_REF =
        "#/components/schemas/CustomerView";

    private static final String VALIDATION_PROBLEM_RESPONSE_REF =
        "#/components/schemas/ValidationProblemResponse";

    private static final String VALIDATION_FAILED_TYPE =
        "urn:kartoush:error:validation-failed";

    private static final String VALIDATION_FAILED_TITLE = "Validation Failed";

    private static final String VALIDATION_FAILED_DETAIL =
        "One or more validation errors occurred.";

    private static final String CUSTOMER_NOT_FOUND_TYPE =
        "urn:kartoush:error:customer-not-found";

    private static final String CUSTOMER_NOT_FOUND_TITLE =
        "Customer Not Found";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerFacade customerFacade;

    @Test
    void shouldMatchDocumentedCustomerSuccessResponse() throws Exception {
        when(customerFacade.getCustomer(CUSTOMER_ID)).thenReturn(customerView());

        mockMvc.perform(get(API_DOCS_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath(schemaRefPath(
                CUSTOMER_BY_ID_PATH,
                "get",
                HttpStatus.OK.value()
            )).value(CUSTOMER_VIEW_REF));

        mockMvc.perform(get(CUSTOMER_BY_ID_PATH, CUSTOMER_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID))
            .andExpect(jsonPath("$.firstName").value(FIRST_NAME))
            .andExpect(jsonPath("$.lastName").value(LAST_NAME))
            .andExpect(jsonPath("$.email").value(EMAIL))
            .andExpect(jsonPath("$.phoneNumber").value(PHONE_NUMBER))
            .andExpect(jsonPath("$.status").value(CustomerStatus.ACTIVE.name()));
    }

    @Test
    void shouldMatchDocumentedValidationProblemForCustomerActivation() throws Exception {
        mockMvc.perform(get(API_DOCS_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath(problemSchemaRefPath(
                CUSTOMER_ACTIVATION_PATH,
                "post",
                HttpStatus.BAD_REQUEST.value()
            )).value(VALIDATION_PROBLEM_RESPONSE_REF));

        mockMvc.perform(post(CUSTOMER_ACTIVATION_PATH, CUSTOMER_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "token": "   "
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value(VALIDATION_FAILED_TYPE))
            .andExpect(jsonPath("$.title").value(VALIDATION_FAILED_TITLE))
            .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.detail").value(VALIDATION_FAILED_DETAIL))
            .andExpect(jsonPath("$.instance").value(CUSTOMER_ACTIVATION_RUNTIME_PATH))
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_FAILED.name()))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.errors[0].field").value("token"))
            .andExpect(jsonPath("$.errors[0].message").value("token must not be blank"))
            .andExpect(jsonPath("$.errors[0].code").value("NotBlank"))
            .andExpect(jsonPath("$.errors[0].rejectedValue").value("[REDACTED]"));
    }

    @Test
    void shouldMatchDocumentedApiProblemForMissingCustomer() throws Exception {
        when(customerFacade.getCustomer(CUSTOMER_ID))
            .thenThrow(new CustomerNotFoundException(CUSTOMER_ID));

        mockMvc.perform(get(API_DOCS_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath(problemSchemaRefPath(
                CUSTOMER_BY_ID_PATH,
                "get",
                HttpStatus.NOT_FOUND.value()
            )).value(API_PROBLEM_RESPONSE_REF));

        mockMvc.perform(get(CUSTOMER_BY_ID_PATH, CUSTOMER_ID))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value(CUSTOMER_NOT_FOUND_TYPE))
            .andExpect(jsonPath("$.title").value(CUSTOMER_NOT_FOUND_TITLE))
            .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
            .andExpect(jsonPath("$.detail").value("Customer not found for id: " + CUSTOMER_ID))
            .andExpect(jsonPath("$.instance").value(CUSTOMER_BY_ID_RUNTIME_PATH))
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.CUSTOMER_NOT_FOUND.name()))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
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

    private static String operationPath(
        final String endpoint,
        final String method,
        final String suffix
    ) {
        return "$.paths['" + endpoint + "']." + method + "." + suffix;
    }

    private static CustomerView customerView() {
        return new CustomerView(
            CUSTOMER_ID,
            FIRST_NAME,
            LAST_NAME,
            EMAIL,
            PHONE_NUMBER,
            CustomerStatus.ACTIVE
        );
    }
}
