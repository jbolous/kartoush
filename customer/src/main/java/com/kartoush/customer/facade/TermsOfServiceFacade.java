package com.kartoush.customer.facade;

import com.kartoush.customer.facade.model.TermsOfServiceView;

public interface TermsOfServiceFacade {

    TermsOfServiceView getCurrentTermsOfService();

    TermsOfServiceView getTermsOfServiceByVersion(String version);
}
