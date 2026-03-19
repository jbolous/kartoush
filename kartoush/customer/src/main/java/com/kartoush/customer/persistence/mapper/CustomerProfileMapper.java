package com.kartoush.customer.persistence.mapper;

import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.persistence.entity.CustomerProfileEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerProfileMapper {

    public CustomerProfileEntity toEntity(final CustomerProfile profile) {
        return new CustomerProfileEntity(
                profile.firstName(),
                profile.lastName(),
                profile.phoneNumber());
    }

    public CustomerProfile toDomain(final CustomerProfileEntity entity) {
        return new CustomerProfile(
                entity.getFirstName(),
                entity.getLastName(),
                entity.getPhoneNumber());
    }
}
