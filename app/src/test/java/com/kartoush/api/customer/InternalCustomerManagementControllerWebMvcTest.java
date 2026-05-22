package com.kartoush.api.customer;

import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.customer.facade.CustomerFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalCustomerManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
    SpringDocConfiguration.class,
    SpringDocSpecPropertiesConfiguration.class,
    SpringDocWebMvcConfiguration.class,
    MultipleOpenApiSupportConfiguration.class
})
class InternalCustomerManagementControllerWebMvcTest {

    private static final String BASE_URL = "/internal/customers";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiProblemFactory apiProblemFactory;

    @MockitoBean
    private CustomerFacade customerFacade;

    @Test
    void shouldGetCustomers() throws Exception {
        when(customerFacade.getCustomers()).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isOk());

        verify(customerFacade).getCustomers();
    }
}
