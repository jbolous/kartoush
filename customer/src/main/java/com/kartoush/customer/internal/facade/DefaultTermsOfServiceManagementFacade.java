package com.kartoush.customer.internal.facade;

import com.kartoush.customer.facade.TermsOfServiceManagementFacade;
import com.kartoush.customer.facade.model.TermsOfServiceManagementView;
import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.service.TermsOfServiceManagementService;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class DefaultTermsOfServiceManagementFacade implements TermsOfServiceManagementFacade {

    private final TermsOfServiceManagementService termsOfServiceManagementService;

    public DefaultTermsOfServiceManagementFacade(final TermsOfServiceManagementService termsOfServiceManagementService) {
        this.termsOfServiceManagementService = termsOfServiceManagementService;
    }

    @Override
    public TermsOfServiceManagementView createDraft(
        final String version,
        final String content,
        final TermsOfServiceContentType contentType) {
        return toView(termsOfServiceManagementService.createDraft(
            version,
            content,
            contentType
        ));
    }

    @Override
    public TermsOfServiceManagementView updateDraft(
        final String termsOfServiceId,
        final String content,
        final TermsOfServiceContentType contentType) {
        return toView(termsOfServiceManagementService.updateDraft(
            termsOfServiceId,
            content,
            contentType
        ));
    }

    @Override
    public TermsOfServiceManagementView schedule(
        final String termsOfServiceId,
        final Instant effectiveAt) {
        return toView(termsOfServiceManagementService.schedule(termsOfServiceId, effectiveAt));
    }

    @Override
    public TermsOfServiceManagementView unschedule(final String termsOfServiceId) {
        return toView(termsOfServiceManagementService.unschedule(termsOfServiceId));
    }

    @Override
    public TermsOfServiceManagementView activateNow(final String termsOfServiceId) {
        return toView(termsOfServiceManagementService.activateNow(termsOfServiceId));
    }

    @Override
    public TermsOfServiceManagementView promoteDueScheduledTerms() {
        return toView(termsOfServiceManagementService.promoteDueScheduledTerms());
    }

    private TermsOfServiceManagementView toView(final TermsOfServiceEntity termsOfService) {
        return new TermsOfServiceManagementView(
            termsOfService.getId(),
            termsOfService.getVersion(),
            termsOfService.getContent(),
            termsOfService.getContentType(),
            termsOfService.getStatus(),
            termsOfService.getEffectiveAt(),
            termsOfService.getSupersededAt(),
            termsOfService.getCreatedAt(),
            termsOfService.getUpdatedAt()
        );
    }
}
