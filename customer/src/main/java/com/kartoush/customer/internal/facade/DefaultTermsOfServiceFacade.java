package com.kartoush.customer.internal.facade;

import com.kartoush.customer.exception.CurrentTermsOfServiceNotFoundException;
import com.kartoush.customer.exception.TermsOfServiceVersionNotFoundException;
import com.kartoush.customer.facade.TermsOfServiceFacade;
import com.kartoush.customer.facade.model.TermsOfServiceView;
import com.kartoush.customer.internal.registration.TermsOfServiceCatalog;
import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.persistence.repository.TermsOfServiceRepository;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import org.springframework.stereotype.Service;

@Service
public class DefaultTermsOfServiceFacade implements TermsOfServiceFacade {

    private final TermsOfServiceCatalog termsOfServiceCatalog;
    private final TermsOfServiceRepository termsOfServiceRepository;

    public DefaultTermsOfServiceFacade(
        final TermsOfServiceCatalog termsOfServiceCatalog,
        final TermsOfServiceRepository termsOfServiceRepository) {
        this.termsOfServiceCatalog = termsOfServiceCatalog;
        this.termsOfServiceRepository = termsOfServiceRepository;
    }

    @Override
    public TermsOfServiceView getCurrentTermsOfService() {
        final TermsOfServiceEntity termsOfService;

        try {
            termsOfService = termsOfServiceCatalog.currentTerms();
        } catch (final IllegalStateException exception) {
            throw new CurrentTermsOfServiceNotFoundException();
        }

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
