package com.kartoush.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.api.auth.AuthenticationService;
import com.kartoush.api.auth.AuthenticationController;
import com.kartoush.api.auth.PasswordResetService;
import com.kartoush.api.auth.SignInRequest;
import com.kartoush.api.auth.SignInView;
import com.kartoush.api.customer.CustomerController;
import com.kartoush.api.error.ApiExceptionHandler;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.api.terms.TermsOfServiceController;
import com.kartoush.config.security.SecurityConfiguration;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.TermsOfServiceFacade;
import com.kartoush.customer.facade.model.TermsOfServiceView;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
    AuthenticationController.class,
    CustomerController.class,
    TermsOfServiceController.class
})
@AutoConfigureMockMvc
@Import({
    ApiExceptionHandler.class,
    ApiProblemFactory.class,
    SecurityConfiguration.class
})
class SecurityConfigurationTest {

    private static final String SIGN_IN_PATH = "/api/auth/sign-in";
    private static final String CUSTOMER_LIST_PATH = "/api/customers";
    private static final String PUBLIC_TERMS_PATH = "/api/terms-of-service/current";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private AuthenticationService customerAuthenticationApplicationService;

    @MockitoBean
    private PasswordResetService customerPasswordResetApplicationService;

    @MockitoBean
    private CustomerFacade customerFacade;

    @MockitoBean
    private TermsOfServiceFacade termsOfServiceFacade;

    @Test
    void shouldAllowAnonymousSignIn() throws Exception {
        final SignInRequest request = new SignInRequest("jack@kartoush.com", "Password123!");
        final SignInView response = new SignInView("opaque-token", "Bearer");

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
    void shouldAllowAnonymousPublicTermsAccess() throws Exception {
        when(termsOfServiceFacade.getCurrentTermsOfService()).thenReturn(new TermsOfServiceView(
            "2026.04.01",
            "Terms content",
            TermsOfServiceContentType.PLAIN_TEXT,
            TermsOfServiceStatus.ACTIVE,
            Instant.parse("2026-04-01T00:00:00Z"),
            null
        ));

        mockMvc.perform(get(PUBLIC_TERMS_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value("2026.04.01"));
    }

    @Test
    void shouldAllowAnonymousCustomerListAccess() throws Exception {
        when(customerFacade.getCustomers()).thenReturn(java.util.List.of());

        mockMvc.perform(get(CUSTOMER_LIST_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
