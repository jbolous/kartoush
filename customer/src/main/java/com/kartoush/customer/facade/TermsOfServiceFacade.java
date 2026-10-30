package com.kartoush.customer.facade;

import com.kartoush.customer.facade.model.TermsOfServiceView;
import com.kartoush.customer.facade.model.TermsOfServiceMetadataView;

public interface TermsOfServiceFacade {

    TermsOfServiceMetadataView getCurrentTermsOfServiceMetadata();

    TermsOfServiceMetadataView getTermsOfServiceMetadataByVersion(String version);

    TermsOfServiceView getCurrentTermsOfService();

    TermsOfServiceView getTermsOfServiceByVersion(String version);
}
