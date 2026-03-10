package com.kartoush.customer.persistence.mapper;

import com.kartoush.customer.domain.CustomerAddress;
import com.kartoush.customer.persistence.entity.CustomerAddressEntity;
import com.kartoush.customer.persistence.model.AddressIdEmbeddable;
import com.kartoush.platform.types.AddressId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CustomerAddressMapper {

    // Entity -> Domain
    @Mapping(target = "customer", ignore = true) // avoid cycles; CustomerMapper handles parent association
    @Mapping(target = "defaultShipping", source = "defaultShipping")
    @Mapping(target = "defaultBilling", source = "defaultBilling")
    @Mapping(target = "withDefaultShipping", ignore = true)
    @Mapping(target = "withDefaultBilling", ignore = true)
    @Mapping(target = "id", source = "id", qualifiedByName = "toDomainAddressId")
    CustomerAddress toDomain(CustomerAddressEntity entity);

    // Domain -> Entity
    @Mapping(target = "customer", ignore = true) // set in CustomerMapper @AfterMapping
    @Mapping(target = "defaultShipping", source = "defaultShipping")
    @Mapping(target = "defaultBilling", source = "defaultBilling")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", source = "id", qualifiedByName = "toEmbeddableAddressId")
    CustomerAddressEntity toEntity(CustomerAddress domain);

    @Named("toEmbeddableAddressId")
    default AddressIdEmbeddable toEmbeddableAddressId(AddressId id) {
        return id == null ? null : AddressIdEmbeddable.from(id);
    }

    @Named("toDomainAddressId")
    default AddressId toDomainAddressId(AddressIdEmbeddable id) {
        return id == null ? null : id.toAddressId();
    }
}
