package com.kartoush.customer.internal.registration;

import org.springframework.stereotype.Component;

@Component
public class TermsOfServicePolicy {

    private static final String CURRENT_TERMS_VERSION = "2026-04";

    public String currentVersion() {
        return CURRENT_TERMS_VERSION;
    }
}
