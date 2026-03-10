package com.kartoush.customer.persistence;

import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.repository.CustomerRepository;
import com.kartoush.platform.ulid.DefaultUlidGenerator;
import com.kartoush.platform.ulid.UlidGenerator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@EnableJpaRepositories(basePackageClasses = CustomerRepository.class)
@EntityScan(basePackageClasses = CustomerEntity.class)
public class CustomerJpaTestConfig {

    @Bean
    public UlidGenerator ulidGenerator() {
        return new DefaultUlidGenerator();
    }
}
