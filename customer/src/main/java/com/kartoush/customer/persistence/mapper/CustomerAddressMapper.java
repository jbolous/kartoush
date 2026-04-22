package com.kartoush.customer.persistence.mapper;

import com.kartoush.customer.domain.CustomerAddress;
import com.kartoush.customer.persistence.entity.CustomerAddressEntity;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.model.AddressIdEmbeddable;
import com.kartoush.platform.types.AddressId;
import org.springframework.stereotype.Component;

@Component
public class CustomerAddressMapper {

    public CustomerAddress toDomain(final CustomerAddressEntity entity) {
        if (entity == null) {
            return null;
        }

        return CustomerAddress.fromPersistence(
            toDomainAddressId(entity.getId()),
            entity.getLabel(),
            entity.getLine1(),
            entity.getLine2(),
            entity.getCity(),
            entity.getStateOrProvince(),
            entity.getPostalCode(),
            entity.getCountryCode(),
            entity.isDefaultShipping(),
            entity.isDefaultBilling()
        );
    }

    public CustomerAddressEntity toEntity(final CustomerAddress domain, final CustomerEntity customer) {
        if (domain == null) {
            return null;
        }

        return CustomerAddressEntity.of(
            toEmbeddableAddressId(domain.getId()),
            customer,
            domain.getLabel(),
            domain.getLine1(),
            domain.getLine2(),
            domain.getCity(),
            domain.getStateOrProvince(),
            domain.getPostalCode(),
            domain.getCountryCode(),
            domain.isDefaultShipping(),
            domain.isDefaultBilling()
        );
    }

    AddressIdEmbeddable toEmbeddableAddressId(final AddressId id) {
        return id == null ? null : AddressIdEmbeddable.from(id);
    }

    AddressId toDomainAddressId(final AddressIdEmbeddable id) {
        return id == null ? null : id.toAddressId();
    }
}
