package com.kartoush.customer.facade.model;

import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import java.time.Instant;

public record TermsOfServiceView(
    String version,
    String content,
    TermsOfServiceContentType contentType,
    TermsOfServiceStatus status,
    Instant effectiveAt,
    Instant supersededAt) {
}
