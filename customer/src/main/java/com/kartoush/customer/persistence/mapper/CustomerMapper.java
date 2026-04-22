package com.kartoush.customer.persistence.mapper;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.customer.persistence.entity.CustomerAddressEntity;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.entity.CustomerProfileEntity;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.platform.types.CustomerId;
import java.util.List;

import com.kartoush.platform.types.Email;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper
{
    private final CustomerAddressMapper customerAddressMapper;

    public CustomerMapper(final CustomerAddressMapper customerAddressMapper) {
        this.customerAddressMapper = customerAddressMapper;
    }

    public CustomerEntity toEntity(final Customer domain)
    {
        if (domain == null) {
            return null;
        }

        final CustomerEntity customerEntity = CustomerEntity.newCustomer(
                toEmbeddableCustomerId(domain.getId()),
                toProfileEntity(domain.getProfile()),
                domain.getEmail().value(),
                domain.getPasswordHash(),
                domain.getStatus()
        );

        final List<CustomerAddressEntity> addressEntities = domain.getAddresses().stream()
                .map(address -> customerAddressMapper.toEntity(address, customerEntity))
                .toList();

        customerEntity.replaceAddresses(addressEntities);

        return customerEntity;
    }

    public Customer toDomain(final CustomerEntity entity)
    {
        if (entity == null) {
            return null;
        }

        return Customer.fromPersistence(
                toDomainCustomerId(entity.getCustomerId()),
                toProfileDomain(entity.getProfile()),
                new Email(entity.getEmail()),
                entity.getPasswordHash(),
                entity.getCustomerStatus(),
                entity.getAddresses().stream()
                        .map(customerAddressMapper::toDomain)
                        .toList());
    }

    public void updateEntity(final Customer domain, final CustomerEntity entity)
    {
        entity.setProfile(toProfileEntity(domain.getProfile()));
        entity.setEmail(domain.getEmail().value());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setCustomerStatus(domain.getStatus());

        final List<CustomerAddressEntity> addressEntities = domain.getAddresses().stream()
                .map(address -> customerAddressMapper.toEntity(address, entity))
                .toList();

        entity.replaceAddresses(addressEntities);
    }

    private CustomerIdEmbeddable toEmbeddableCustomerId(final CustomerId id)
    {
        return id == null ? null : CustomerIdEmbeddable.from(id);
    }

    private CustomerId toDomainCustomerId(final CustomerIdEmbeddable id)
    {
        return id == null ? null : id.toCustomerId();
    }

    private CustomerProfileEntity toProfileEntity(final CustomerProfile profile) {
        if (profile == null) {
            return null;
        }

        return new CustomerProfileEntity(
            profile.firstName(),
            profile.lastName(),
            profile.phoneNumber()
        );
    }

    private CustomerProfile toProfileDomain(final CustomerProfileEntity entity) {
        if (entity == null) {
            return null;
        }

        return new CustomerProfile(
            entity.getFirstName(),
            entity.getLastName(),
            entity.getPhoneNumber()
        );
    }
}
