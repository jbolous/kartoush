package com.kartoush.customer.service;

import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import java.time.Instant;

public interface TermsOfServiceManagementService {

    TermsOfServiceEntity createDraft(
        String version,
        String content,
        TermsOfServiceContentType contentType);

    TermsOfServiceEntity updateDraft(
        String termsOfServiceId,
        String content,
        TermsOfServiceContentType contentType);

    TermsOfServiceEntity schedule(
        String termsOfServiceId,
        Instant effectiveAt);

    TermsOfServiceEntity unschedule(String termsOfServiceId);

    TermsOfServiceEntity activateNow(String termsOfServiceId);

    TermsOfServiceEntity promoteDueScheduledTerms();
}
