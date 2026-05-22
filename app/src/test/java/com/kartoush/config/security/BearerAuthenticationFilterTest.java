package com.kartoush.config.security;

import com.kartoush.auth.domain.ActiveSession;
import com.kartoush.auth.service.CustomerAuthSessionService;
import com.kartoush.customer.facade.CustomerAuthenticationFacade;
import com.kartoush.customer.facade.model.AuthCandidateView;
import com.kartoush.platform.types.CustomerStatus;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BearerAuthenticationFilterTest {

    private static final String CUSTOMER_ID = "01KQ0CUSTOMER0000000000001";
    private static final String CUSTOMER_EMAIL = "jack@kartoush.com";
    private static final String CUSTOMERS_PATH = "/api/customers";
    private static final String ACCESS_TOKEN = "opaque-token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_AUTHORIZATION = "Bearer " + ACCESS_TOKEN;
    private static final String LOWERCASE_BEARER_AUTHORIZATION = "bearer " + ACCESS_TOKEN;

    private final CustomerAuthSessionService customerAuthSessionService = mock(CustomerAuthSessionService.class);
    private final CustomerAuthenticationFacade customerAuthenticationFacade = mock(CustomerAuthenticationFacade.class);
    private final BearerAuthenticationFilter filter =
        new BearerAuthenticationFilter(customerAuthSessionService, customerAuthenticationFacade);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateActiveCustomerFromBearerToken() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", CUSTOMERS_PATH);
        request.setServletPath(CUSTOMERS_PATH);
        request.addHeader(AUTHORIZATION_HEADER, BEARER_AUTHORIZATION);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        when(customerAuthSessionService.findActiveCustomerByAccessToken(ACCESS_TOKEN))
            .thenReturn(Optional.of(new ActiveSession("01KQ0SESSION0000000000001", CUSTOMER_ID)));
        when(customerAuthenticationFacade.findAuthCandidateById(CUSTOMER_ID))
            .thenReturn(Optional.of(new AuthCandidateView(CUSTOMER_ID, CUSTOMER_EMAIL, CustomerStatus.ACTIVE)));

        filter.doFilter(request, response, filterChain);

        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentication.getPrincipal()).isEqualTo(new AuthenticatedPrincipal(CUSTOMER_ID, CUSTOMER_EMAIL));
        assertThat(authentication.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_CUSTOMER");
        verify(customerAuthSessionService).findActiveCustomerByAccessToken(ACCESS_TOKEN);
        verify(customerAuthenticationFacade).findAuthCandidateById(CUSTOMER_ID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldLeaveSecurityContextEmptyWhenBearerTokenIsUnknown() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", CUSTOMERS_PATH);
        request.setServletPath(CUSTOMERS_PATH);
        request.addHeader(AUTHORIZATION_HEADER, BEARER_AUTHORIZATION);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        when(customerAuthSessionService.findActiveCustomerByAccessToken(ACCESS_TOKEN))
            .thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(customerAuthSessionService).findActiveCustomerByAccessToken(ACCESS_TOKEN);
        verifyNoInteractions(customerAuthenticationFacade);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateWhenBearerSchemeIsLowercase() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", CUSTOMERS_PATH);
        request.setServletPath(CUSTOMERS_PATH);
        request.addHeader(AUTHORIZATION_HEADER, LOWERCASE_BEARER_AUTHORIZATION);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        when(customerAuthSessionService.findActiveCustomerByAccessToken(ACCESS_TOKEN))
            .thenReturn(Optional.of(new ActiveSession("01KQ0SESSION0000000000001", CUSTOMER_ID)));
        when(customerAuthenticationFacade.findAuthCandidateById(CUSTOMER_ID))
            .thenReturn(Optional.of(new AuthCandidateView(CUSTOMER_ID, CUSTOMER_EMAIL, CustomerStatus.ACTIVE)));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(customerAuthSessionService).findActiveCustomerByAccessToken(ACCESS_TOKEN);
        verify(customerAuthenticationFacade).findAuthCandidateById(CUSTOMER_ID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldFilterApiRequestWhenContextPathIsPresent() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/kartoush" + CUSTOMERS_PATH);
        request.setContextPath("/kartoush");
        request.setServletPath(CUSTOMERS_PATH);
        request.addHeader(AUTHORIZATION_HEADER, BEARER_AUTHORIZATION);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        when(customerAuthSessionService.findActiveCustomerByAccessToken(ACCESS_TOKEN))
            .thenReturn(Optional.of(new ActiveSession("01KQ0SESSION0000000000001", CUSTOMER_ID)));
        when(customerAuthenticationFacade.findAuthCandidateById(CUSTOMER_ID))
            .thenReturn(Optional.of(new AuthCandidateView(CUSTOMER_ID, CUSTOMER_EMAIL, CustomerStatus.ACTIVE)));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(customerAuthSessionService).findActiveCustomerByAccessToken(ACCESS_TOKEN);
        verify(customerAuthenticationFacade).findAuthCandidateById(CUSTOMER_ID);
        verify(filterChain).doFilter(request, response);
    }
}
