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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
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
class CustomerOpenApiWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerFacade customerFacade;

    @MockitoBean
    private ApiProblemFactory apiProblemFactory;

    @Test
    void shouldExposeCustomerOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("Kartoush API"))
            .andExpect(jsonPath("$.paths['/api/customers'].post.summary").value("Create a customer"))
            .andExpect(jsonPath("$.paths['/api/customers'].post.responses['400'].content['application/problem+json'].schema['$ref']")
                .value("#/components/schemas/ValidationProblemResponse"))
            .andExpect(jsonPath("$.paths['/api/customers/{customerId}'].put.responses['409'].content['application/problem+json'].schema['$ref']")
                .value("#/components/schemas/ApiProblemResponse"))
            .andExpect(jsonPath("$.paths['/api/customers/{customerId}'].get.parameters[0].description")
                .value("Customer ULID identifier"))
            .andExpect(jsonPath("$.components.schemas.ValidationProblemResponse.properties.errors.type").value("array"))
            .andExpect(jsonPath("$.components.schemas.CustomerView.properties.status.description")
                .value("Customer lifecycle status"));
    }
}
