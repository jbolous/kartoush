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
    private static final String PASSWORD_RESET_PATH = "/api/auth/password-reset";
    private static final String PASSWORD_RESET_CONFIRM_PATH = "/api/auth/password-reset/confirm";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ApiProblemFactory apiProblemFactory;

    @MockitoBean
    private CustomerAuthenticationApplicationService customerAuthenticationApplicationService;

    @MockitoBean
    private CustomerPasswordResetApplicationService customerPasswordResetApplicationService;

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

    @Test
    void shouldRequestPasswordReset() throws Exception {
        final ForgotCustomerPasswordRequest request = new ForgotCustomerPasswordRequest("jack@kartoush.com");

        mockMvc.perform(post(PASSWORD_RESET_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(customerPasswordResetApplicationService).requestPasswordReset(request.email());
    }

    @Test
    void shouldResetCustomerPassword() throws Exception {
        final ResetCustomerPasswordRequest request =
            new ResetCustomerPasswordRequest("jack@kartoush.com", "reset-token", "Password123!", "Password123!");

        mockMvc.perform(post(PASSWORD_RESET_CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(customerPasswordResetApplicationService).resetPassword(request);
    }
}
