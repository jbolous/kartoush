package com.kartoush.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI kartoushOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Kartoush API")
                .version("v1")
                .description("Public and internal HTTP APIs for customer registration, lifecycle management, and Terms of Service workflows.")
                .contact(new Contact().name("Kartoush")));
    }
}
