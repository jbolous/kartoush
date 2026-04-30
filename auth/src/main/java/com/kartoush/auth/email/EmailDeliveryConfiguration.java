package com.kartoush.auth.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailDeliveryConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmailDeliveryService.class)
    EmailDeliveryService emailDeliveryService() {
        return new NoOpEmailDeliveryService();
    }
}
