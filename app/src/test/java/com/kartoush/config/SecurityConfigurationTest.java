package com.kartoush.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.api.auth.AuthenticationService;
import com.kartoush.api.auth.AuthenticationController;
import com.kartoush.api.auth.PasswordResetService;
import com.kartoush.api.auth.SignInRequest;
import com.kartoush.api.auth.SignInView;
import com.kartoush.auth.service.CustomerAuthSessionService;
import com.kartoush.api.customer.CustomerController;
import com.kartoush.api.error.ApiExceptionHandler;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.api.terms.InternalTermsOfServiceManagementController;
import com.kartoush.api.terms.TermsOfServiceController;
import com.kartoush.config.security.ApiAccessDeniedHandler;
import com.kartoush.config.security.ApiAuthenticationEntryPoint;
import com.kartoush.config.security.SecurityConfiguration;
import com.kartoush.customer.facade.CustomerAuthenticationFacade;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.TermsOfServiceFacade;
import com.kartoush.customer.facade.TermsOfServiceManagementFacade;
import com.kartoush.customer.facade.model.TermsOfServiceManagementView;
import com.kartoush.customer.facade.model.TermsOfServiceView;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
    AuthenticationController.class,
    CustomerController.class,
    TermsOfServiceController.class,
    InternalTermsOfServiceManagementController.class
})
@AutoConfigureMockMvc
@Import({
    ApiExceptionHandler.class,
    ApiProblemFactory.class,
    ApiAuthenticationEntryPoint.class,
    ApiAccessDeniedHandler.class,
    SecurityConfiguration.class,
    SecurityConfigurationTest.TestSecurityUsers.class
})
class SecurityConfigurationTest {

    private static final String SIGN_IN_PATH = "/api/auth/sign-in";
    private static final String CUSTOMER_LIST_PATH = "/api/customers";
    private static final String CUSTOMER_DETAILS_PATH = "/api/customers/01KQ0CUSTOMER0000000000001";
    private static final String PUBLIC_TERMS_PATH = "/api/terms-of-service/current";
    private static final String INTERNAL_TERMS_DRAFTS_PATH = "/internal/terms-of-service/drafts";
    private static final String ACCESS_TOKEN = "opaque-token";
    private static final String TOKEN_TYPE = "Bearer";
    private static final String BEARER_CHALLENGE = "Bearer";
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    private static final String BASIC_CHALLENGE = "Basic realm=\"Kartoush Internal\"";
    private static final String AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED";
    private static final String AUTHENTICATION_REQUIRED_TITLE = "Authentication Required";
    private static final String CUSTOMER_EMAIL = "jack@kartoush.com";
    private static final String INTERNAL_ADMIN_USERNAME = "internal-admin";
    private static final String INTERNAL_ADMIN_PASSWORD = "test-internal-admin-password";
    private static final String INTERNAL_TERMS_DRAFT_REQUEST = """
        {
          "version": "2026.06.01",
          "content": "Draft terms",
          "contentType": "MARKDOWN"
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private CustomerAuthSessionService customerAuthSessionService;

    @MockitoBean
    private CustomerAuthenticationFacade customerAuthenticationFacade;

    @MockitoBean
    private CustomerFacade customerFacade;

    @MockitoBean
    private TermsOfServiceFacade termsOfServiceFacade;

    @MockitoBean
    private TermsOfServiceManagementFacade termsOfServiceManagementFacade;

    @Test
    void shouldAllowAnonymousSignIn() throws Exception {
        final SignInRequest request = new SignInRequest("jack@kartoush.com", "Password123!");
        final SignInView response = new SignInView(ACCESS_TOKEN, TOKEN_TYPE);

        when(authenticationService.signIn(eq(request.email()), eq(request.password())))
            .thenReturn(response);

        mockMvc.perform(post(SIGN_IN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
            .andExpect(jsonPath("$.tokenType").value(TOKEN_TYPE));

        verify(authenticationService).signIn(request.email(), request.password());
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
    void shouldAllowAnonymousHeadPublicTermsAccess() throws Exception {
        mockMvc.perform(head(PUBLIC_TERMS_PATH))
            .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAnonymousCustomerListAccess() throws Exception {
        mockMvc.perform(get(CUSTOMER_LIST_PATH))
            .andExpect(status().isUnauthorized())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string(WWW_AUTHENTICATE_HEADER, BASIC_CHALLENGE))
            .andExpect(jsonPath("$.errorCode").value(AUTHENTICATION_REQUIRED))
            .andExpect(jsonPath("$.title").value(AUTHENTICATION_REQUIRED_TITLE));
    }

    @Test
    void shouldRejectAnonymousInternalTermsAccess() throws Exception {
        mockMvc.perform(post(INTERNAL_TERMS_DRAFTS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(INTERNAL_TERMS_DRAFT_REQUEST))
            .andExpect(status().isUnauthorized())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string(WWW_AUTHENTICATE_HEADER, BASIC_CHALLENGE))
            .andExpect(jsonPath("$.errorCode").value(AUTHENTICATION_REQUIRED))
            .andExpect(jsonPath("$.title").value(AUTHENTICATION_REQUIRED_TITLE));
    }

    @Test
    void shouldRejectCustomerAccessToCustomerList() throws Exception {
        when(customerFacade.getCustomers()).thenReturn(List.of());

        mockMvc.perform(get(CUSTOMER_LIST_PATH)
                .with(user(CUSTOMER_EMAIL).roles("CUSTOMER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAdminAccessToCustomerDetailsUsingHttpBasic() throws Exception {
        mockMvc.perform(get(CUSTOMER_DETAILS_PATH)
                .with(httpBasic(INTERNAL_ADMIN_USERNAME, INTERNAL_ADMIN_PASSWORD)))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminCustomerListAccessUsingHttpBasic() throws Exception {
        when(customerFacade.getCustomers()).thenReturn(List.of());

        mockMvc.perform(get(CUSTOMER_LIST_PATH)
                .with(httpBasic(INTERNAL_ADMIN_USERNAME, INTERNAL_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        verify(customerFacade).getCustomers();
    }

    @Test
    void shouldAllowAdminInternalTermsAccessUsingHttpBasic() throws Exception {
        when(termsOfServiceManagementFacade.createDraft(any(), any(), any()))
            .thenReturn(new TermsOfServiceManagementView(
                "01KQ0INTERNALTERMS000000001",
                "2026.06.01",
                "Draft terms",
                TermsOfServiceContentType.MARKDOWN,
                TermsOfServiceStatus.DRAFT,
                null,
                null,
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:00Z")
            ));

        mockMvc.perform(post(INTERNAL_TERMS_DRAFTS_PATH)
                .with(httpBasic(INTERNAL_ADMIN_USERNAME, INTERNAL_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(INTERNAL_TERMS_DRAFT_REQUEST))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @TestConfiguration
    static class TestSecurityUsers {

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(User.withUsername(INTERNAL_ADMIN_USERNAME)
                .password("{noop}" + INTERNAL_ADMIN_PASSWORD)
                .roles("ADMIN")
                .build());
        }
    }
}
