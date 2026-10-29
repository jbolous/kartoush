package com.kartoush.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class ApiResponseCacheConfiguration {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> termsResponseEtagFilter() {
        final FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
            new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/terms-of-service/*");
        return registration;
    }
}
