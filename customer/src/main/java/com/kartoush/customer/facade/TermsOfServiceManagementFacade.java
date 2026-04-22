package com.kartoush.customer.facade;

import com.kartoush.customer.facade.model.TermsOfServiceManagementView;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import java.time.Instant;

public interface TermsOfServiceManagementFacade {

    TermsOfServiceManagementView createDraft(String version, String content, TermsOfServiceContentType contentType);

    TermsOfServiceManagementView updateDraft(
        String termsOfServiceId,
        String content,
        TermsOfServiceContentType contentType);

    TermsOfServiceManagementView schedule(String termsOfServiceId, Instant effectiveAt);

    TermsOfServiceManagementView unschedule(String termsOfServiceId);

    TermsOfServiceManagementView activateNow(String termsOfServiceId);

    TermsOfServiceManagementView promoteDueScheduledTerms();
}
