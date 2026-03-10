package com.kartoush.customer.persistence.mapper;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.persistence.entity.CustomerAddressEntity;
import com.kartoush.customer.persistence.entity.CustomerEntity;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.platform.types.CustomerId;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {CustomerAddressMapper.class})
public interface CustomerMapper {

    // Domain -> Entity
    @Mapping(target = "id", source = "id", qualifiedByName = "toEmbeddableCustomerId")
    @Mapping(target = "profile", source = "profile")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CustomerEntity toEntity(Customer domain);

    // Entity -> Domain
    @Mapping(target = "id", source = "id", qualifiedByName = "toDomainCustomerId")
    @Mapping(target = "profile", source = "profile")
    Customer toDomain(CustomerEntity entity);

    @AfterMapping
    default void linkAddresses(@MappingTarget CustomerEntity customerEntity) {
        if (customerEntity.getAddresses() == null) return;

        for (CustomerAddressEntity address : customerEntity.getAddresses()) {
            address.setCustomer(customerEntity);
            if (address.getId() == null) {
                throw new IllegalStateException("Address id must be set before persisting");
            }
        }
    }

    @Named("toEmbeddableCustomerId")
    default CustomerIdEmbeddable toEmbeddableCustomerId(CustomerId id) {
        return id == null ? null : CustomerIdEmbeddable.from(id);
    }

    @Named("toDomainCustomerId")
    default CustomerId toDomainCustomerId(CustomerIdEmbeddable id) {
        return id == null ? null : id.toCustomerId();
    }
}


