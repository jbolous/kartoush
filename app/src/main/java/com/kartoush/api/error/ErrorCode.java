package com.kartoush.api.error;

public enum ErrorCode {
    // Customer
    CUSTOMER_ADDRESS_NOT_FOUND,
    CUSTOMER_ALREADY_EXISTS,
    CUSTOMER_DELETED,
    CUSTOMER_NOT_FOUND,
    CUSTOMER_PENDING_ACTIVATION,

    // Customer Activation
    ACTIVATION_TOKEN_NOT_FOUND,
    ACTIVATION_TOKEN_EXPIRED,
    ACTIVATION_TOKEN_CONSUMED,
    INVALID_CUSTOMER_ACTIVATION,
    INVALID_ACTIVATION_TOKEN_RESEND,

    // Customer Lifecycle
    INVALID_CUSTOMER_STATUS_FOR_UPDATE,
    INVALID_CUSTOMER_STATUS_TRANSITION,
    INVALID_CUSTOMER_REACTIVATION,

    // Terms of Service
    TERMS_OF_SERVICE_NOT_FOUND,

    // Validation
    VALIDATION_FAILED,

    // System
    INTERNAL_ERROR;

    private final String urn;

    ErrorCode() {
        this.urn = "urn:kartoush:error:" + name().toLowerCase().replace('_', '-');
    }

    public String urn() {
        return urn;
    }
}
