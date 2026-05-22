package com.kartoush.config.security;

import com.kartoush.auth.service.CustomerAuthSessionService;
import com.kartoush.customer.facade.CustomerAuthenticationFacade;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final String CUSTOMER_ROLE = "ROLE_CUSTOMER";

    private static final String CUSTOMER_PATH_PREFIX = "/api/customers/";

    @Bean
    public BearerAuthenticationFilter bearerAuthenticationFilter(
        final CustomerAuthSessionService customerAuthSessionService,
        final CustomerAuthenticationFacade customerAuthenticationFacade
    ) {
        return new BearerAuthenticationFilter(customerAuthSessionService, customerAuthenticationFacade);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        final HttpSecurity http,
        final ApiAuthenticationEntryPoint authenticationEntryPoint,
        final ApiAccessDeniedHandler accessDeniedHandler,
        final BearerAuthenticationFilter bearerAuthenticationFilter
    ) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(Customizer.withDefaults())
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(bearerAuthenticationFilter, AnonymousAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/favicon.ico",
                    "/favicon.svg",
                    "/static/**",
                    "/assets/**",
                    "/webjars/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/dev/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/terms-of-service/**").permitAll()
                .requestMatchers(HttpMethod.HEAD, "/api/terms-of-service/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/customers").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/customers/*/activation").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/customers/*/initial-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/customers/*/activation/resend").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/sign-in").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/password-reset").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/password-reset/confirm").permitAll()
                .requestMatchers("/api/customers/*", "/api/customers/*/**").access(customerOwnsRequestedRecord())
                .requestMatchers("/internal/**").hasRole("ADMIN")
                .requestMatchers("/api/**").denyAll()
                .anyRequest().permitAll())
            .anonymous(Customizer.withDefaults());

        return http.build();
    }

    private AuthorizationManager<RequestAuthorizationContext> customerOwnsRequestedRecord() {
        return (authenticationSupplier, context) -> new AuthorizationDecision(isAuthorizedCustomerForRequest(
            authenticationSupplier,
            context.getRequest()
        ));
    }

    private boolean isAuthorizedCustomerForRequest(
        final Supplier<? extends Authentication> authenticationSupplier,
        final HttpServletRequest request
    ) {
        final Authentication authentication = authenticationSupplier.get();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (authentication.getAuthorities().stream()
            .noneMatch(authority -> CUSTOMER_ROLE.equals(authority.getAuthority()))) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            return false;
        }

        final String requestedCustomerId = extractCustomerId(request);
        return requestedCustomerId != null && requestedCustomerId.equals(principal.customerId());
    }

    private String extractCustomerId(final HttpServletRequest request) {
        final String requestUri = request.getRequestURI();
        final String contextPath = request.getContextPath();
        final String requestPath = contextPath != null
            && !contextPath.isEmpty()
            && requestUri.startsWith(contextPath)
            ? requestUri.substring(contextPath.length())
            : requestUri;

        if (!requestPath.startsWith(CUSTOMER_PATH_PREFIX)) {
            return null;
        }

        final String remainingPath = requestPath.substring(CUSTOMER_PATH_PREFIX.length());
        final int nextSlash = remainingPath.indexOf('/');
        return nextSlash >= 0 ? remainingPath.substring(0, nextSlash) : remainingPath;
    }
}
