package com.kartoush.customer.persistence.mapper;

import com.kartoush.customer.domain.ActivationToken;
import com.kartoush.customer.persistence.entity.ActivationTokenEntity;
import com.kartoush.customer.persistence.model.ActivationTokenIdEmbeddable;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.platform.types.ActivationTokenId;
import com.kartoush.platform.types.CustomerId;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ActivationTokenMapper {

    default ActivationToken toDomain(ActivationTokenEntity entity) {
        if (entity == null) {
            return null;
        }

        return ActivationToken.fromPersistence(
            entity.getId().toActivationTokenId(),
            entity.getCustomerId().toCustomerId(),
            entity.getTokenHash(),
            entity.getExpiresAt(),
            entity.getConsumedAt(),
            entity.getCreatedAt()
        );
    }

    default ActivationTokenEntity toEntity(ActivationToken domain) {
        if (domain == null) {
            return null;
        }

        return ActivationTokenEntity.of(
            toEmbeddableId(domain.getId()),
            toEmbeddableId(domain.getCustomerId()),
            domain.getTokenHash(),
            domain.getExpiresAt(),
            domain.getConsumedAt(),
            domain.getCreatedAt());
    }

    default ActivationTokenIdEmbeddable toEmbeddableId(ActivationTokenId activationTokenId) {
        if (activationTokenId == null) {
            return null;
        }

        return ActivationTokenIdEmbeddable.from(activationTokenId);
    }

    default CustomerIdEmbeddable toEmbeddableId(CustomerId customerId) {
        if (customerId == null) {
            return null;
        }

        return CustomerIdEmbeddable.from(customerId);
    }
}
