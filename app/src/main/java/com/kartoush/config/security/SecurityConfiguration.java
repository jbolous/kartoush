package com.kartoush.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(
        final HttpSecurity http,
        final ApiAuthenticationEntryPoint authenticationEntryPoint,
        final ApiAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
                .requestMatchers(HttpMethod.POST, "/api/customers").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/customers/*/activation").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/customers/*/initial-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/customers/*/activation/resend").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/sign-in").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/password-reset").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/password-reset/confirm").permitAll()
                .anyRequest().permitAll())
            .anonymous(Customizer.withDefaults());

        return http.build();
    }
}
