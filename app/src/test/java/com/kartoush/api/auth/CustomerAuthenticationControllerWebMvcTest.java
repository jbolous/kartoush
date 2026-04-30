package com.kartoush.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.api.error.ApiProblemFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerAuthenticationController.class)
@ImportAutoConfiguration(exclude = {
    SpringDocConfiguration.class,
    SpringDocSpecPropertiesConfiguration.class,
    SpringDocWebMvcConfiguration.class,
    MultipleOpenApiSupportConfiguration.class
})
class CustomerAuthenticationControllerWebMvcTest {

    private static final String SIGN_IN_PATH = "/api/auth/sign-in";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ApiProblemFactory apiProblemFactory;

    @MockitoBean
    private CustomerAuthenticationApplicationService customerAuthenticationApplicationService;

    @Test
    void shouldSignInCustomer() throws Exception {
        final CustomerSignInRequest request = new CustomerSignInRequest("jack@kartoush.com", "Password123!");
        final CustomerSignInView response = new CustomerSignInView("opaque-token", "Bearer");

        when(customerAuthenticationApplicationService.signIn(eq(request.email()), eq(request.password())))
            .thenReturn(response);

        mockMvc.perform(post(SIGN_IN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("opaque-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(customerAuthenticationApplicationService).signIn(request.email(), request.password());
    }
}
