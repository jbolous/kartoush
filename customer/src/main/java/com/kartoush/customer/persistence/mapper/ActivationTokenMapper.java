package com.kartoush.customer.persistence.mapper;

import com.kartoush.customer.domain.ActivationToken;
import com.kartoush.customer.persistence.entity.ActivationTokenEntity;
import com.kartoush.customer.persistence.model.ActivationTokenIdEmbeddable;
import com.kartoush.customer.persistence.model.CustomerIdEmbeddable;
import com.kartoush.platform.types.ActivationTokenId;
import com.kartoush.platform.types.CustomerId;
import org.springframework.stereotype.Component;

@Component
public class ActivationTokenMapper {

    public ActivationToken toDomain(final ActivationTokenEntity entity) {
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

    public ActivationTokenEntity toEntity(final ActivationToken domain) {
        if (domain == null) {
            return null;
        }

        return ActivationTokenEntity.of(
            toEmbeddableActivationTokenId(domain.getId()),
            toEmbeddableCustomerId(domain.getCustomerId()),
            domain.getTokenHash(),
            domain.getExpiresAt(),
            domain.getConsumedAt(),
            domain.getCreatedAt()
        );
    }

    ActivationTokenIdEmbeddable toEmbeddableActivationTokenId(final ActivationTokenId activationTokenId) {
        if (activationTokenId == null) {
            return null;
        }

        return ActivationTokenIdEmbeddable.from(activationTokenId);
    }

    CustomerIdEmbeddable toEmbeddableCustomerId(final CustomerId customerId) {
        if (customerId == null) {
            return null;
        }

        return CustomerIdEmbeddable.from(customerId);
    }
}
