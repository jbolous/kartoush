package com.kartoush.customer.persistence.model;

import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import java.time.Instant;

public interface TermsOfServiceMetadataProjection {
    String getVersion();
    TermsOfServiceContentType getContentType();
    TermsOfServiceStatus getStatus();
    Instant getEffectiveAt();
    Instant getSupersededAt();
}
