package com.kartoush.config.security;

import com.kartoush.auth.domain.ActiveSession;
import com.kartoush.auth.service.CustomerAuthSessionService;
import com.kartoush.customer.facade.CustomerAuthenticationFacade;
import com.kartoush.customer.facade.model.AuthCandidateView;
import com.kartoush.platform.types.CustomerStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

public class BearerAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CUSTOMER_ROLE = "ROLE_CUSTOMER";

    private final CustomerAuthSessionService customerAuthSessionService;

    private final CustomerAuthenticationFacade customerAuthenticationFacade;

    public BearerAuthenticationFilter(
        final CustomerAuthSessionService customerAuthSessionService,
        final CustomerAuthenticationFacade customerAuthenticationFacade
    ) {
        this.customerAuthSessionService = customerAuthSessionService;
        this.customerAuthenticationFacade = customerAuthenticationFacade;
    }

    @Override
    protected void doFilterInternal(
        final @NonNull HttpServletRequest request,
        final @NonNull HttpServletResponse response,
        final @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveAuthentication(request)
                .ifPresent(authentication -> SecurityContextHolder.getContext().setAuthentication(authentication));
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    private Optional<UsernamePasswordAuthenticationToken> resolveAuthentication(final HttpServletRequest request) {
        final String accessToken = extractBearerToken(request);
        if (accessToken == null) {
            return Optional.empty();
        }

        final Optional<ActiveSession> session =
            customerAuthSessionService.findActiveCustomerByAccessToken(accessToken);
        if (session.isEmpty()) {
            return Optional.empty();
        }

        final Optional<AuthCandidateView> candidate =
            customerAuthenticationFacade.findAuthCandidateById(session.get().customerId());
        if (candidate.isEmpty() || candidate.get().status() != CustomerStatus.ACTIVE) {
            return Optional.empty();
        }

        final AuthenticatedPrincipal principal =
            new AuthenticatedPrincipal(candidate.get().customerId(), candidate.get().email());
        final UsernamePasswordAuthenticationToken authentication =
            UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                AuthorityUtils.createAuthorityList(CUSTOMER_ROLE)
            );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        return Optional.of(authentication);
    }

    private String extractBearerToken(final HttpServletRequest request) {
        final String headerValue = request.getHeader(AUTHORIZATION_HEADER);
        if (headerValue == null || !headerValue.startsWith(BEARER_PREFIX)) {
            return null;
        }

        final String accessToken = headerValue.substring(BEARER_PREFIX.length()).trim();
        return accessToken.isEmpty() ? null : accessToken;
    }
}
