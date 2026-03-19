package com.kartoush.customer.persistence.mapper;

import com.kartoush.customer.domain.CustomerAddress;
import com.kartoush.customer.persistence.entity.CustomerAddressEntity;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.model.AddressIdEmbeddable;
import com.kartoush.platform.types.AddressId;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerAddressMapper {

    // Entity -> Domain
    default CustomerAddress toDomain(final CustomerAddressEntity entity) {
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
                entity.isDefaultBilling());
    }

    // Domain -> Entity
    default CustomerAddressEntity toEntity(
            final CustomerAddress domain,
            final CustomerEntity customer)
    {
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

    default AddressIdEmbeddable toEmbeddableAddressId(AddressId id) {
        return id == null ? null : AddressIdEmbeddable.from(id);
    }

    default AddressId toDomainAddressId(AddressIdEmbeddable id) {
        return id == null ? null : id.toAddressId();
    }
}
