package com.kartoush.customer.internal.registration;

import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;

public interface TermsOfServiceCatalog {

    String currentVersion();

    TermsOfServiceEntity currentTerms();
}
