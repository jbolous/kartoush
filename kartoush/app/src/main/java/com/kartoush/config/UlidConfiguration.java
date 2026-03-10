package com.kartoush.config;

import com.kartoush.platform.ulid.DefaultUlidGenerator;
import com.kartoush.platform.ulid.UlidGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UlidConfiguration {

    @Bean
    public UlidGenerator ulidGenerator() {
        return new DefaultUlidGenerator();
    }
}

