package com.kartoush.customer.facade.model;

import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import java.time.Instant;

/** Published content is immutable per version; these fields capture its mutable public state. */
public record TermsOfServiceMetadataView(String version, TermsOfServiceContentType contentType,
    TermsOfServiceStatus status, Instant effectiveAt, Instant supersededAt) {

    public static TermsOfServiceMetadataView from(final TermsOfServiceView terms) {
        return new TermsOfServiceMetadataView(terms.version(), terms.contentType(), terms.status(),
            terms.effectiveAt(), terms.supersededAt());
    }
}
