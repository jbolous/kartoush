package com.kartoush.customer.internal.facade;

import com.kartoush.customer.exception.CurrentTermsOfServiceNotFoundException;
import com.kartoush.customer.exception.TermsOfServiceVersionNotFoundException;
import com.kartoush.customer.facade.TermsOfServiceFacade;
import com.kartoush.customer.facade.model.TermsOfServiceMetadataView;
import com.kartoush.customer.facade.model.TermsOfServiceView;
import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.persistence.model.TermsOfServiceMetadataProjection;
import com.kartoush.customer.persistence.repository.TermsOfServiceRepository;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DefaultTermsOfServiceFacade implements TermsOfServiceFacade {

    private final TermsOfServiceRepository termsOfServiceRepository;

    public DefaultTermsOfServiceFacade(final TermsOfServiceRepository termsOfServiceRepository) {
        this.termsOfServiceRepository = termsOfServiceRepository;
    }

    @Override
    public TermsOfServiceMetadataView getCurrentTermsOfServiceMetadata() {
        return termsOfServiceRepository.findMetadataByStatus(TermsOfServiceStatus.ACTIVE)
            .map(this::toMetadata)
            .orElseThrow(CurrentTermsOfServiceNotFoundException::new);
    }

    @Override
    public TermsOfServiceMetadataView getTermsOfServiceMetadataByVersion(final String version) {
        return termsOfServiceRepository.findMetadataByVersionAndStatusIn(version,
                Set.of(TermsOfServiceStatus.ACTIVE, TermsOfServiceStatus.SUPERSEDED))
            .map(this::toMetadata)
            .orElseThrow(() -> new TermsOfServiceVersionNotFoundException(version));
    }

    private TermsOfServiceMetadataView toMetadata(final TermsOfServiceMetadataProjection metadata) {
        return new TermsOfServiceMetadataView(metadata.getVersion(), metadata.getContentType(), metadata.getStatus(),
            metadata.getEffectiveAt(), metadata.getSupersededAt());
    }

    @Override
    public TermsOfServiceView getCurrentTermsOfService() {
        final TermsOfServiceEntity termsOfService = termsOfServiceRepository.findByStatus(TermsOfServiceStatus.ACTIVE)
            .orElseThrow(CurrentTermsOfServiceNotFoundException::new);

        return toView(termsOfService);
    }

    @Override
    public TermsOfServiceView getTermsOfServiceByVersion(final String version) {
        final TermsOfServiceEntity termsOfService = termsOfServiceRepository.findByVersion(version)
            .filter(this::isPublished)
            .orElseThrow(() -> new TermsOfServiceVersionNotFoundException(version));

        return toView(termsOfService);
    }

    private boolean isPublished(final TermsOfServiceEntity termsOfService) {
        final TermsOfServiceStatus status = termsOfService.getStatus();
        return status == TermsOfServiceStatus.ACTIVE || status == TermsOfServiceStatus.SUPERSEDED;
    }

    private TermsOfServiceView toView(final TermsOfServiceEntity termsOfService) {
        return new TermsOfServiceView(
            termsOfService.getVersion(),
            termsOfService.getContent(),
            termsOfService.getContentType(),
            termsOfService.getStatus(),
            termsOfService.getEffectiveAt(),
            termsOfService.getSupersededAt()
        );
    }
}
