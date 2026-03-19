package com.kartoush.api.error;

public enum ErrorCode {
    CUSTOMER_NOT_FOUND,
    CUSTOMER_ALREADY_EXISTS,
    CUSTOMER_ADDRESS_NOT_FOUND,
    VALIDATION_FAILED,
    CUSTOMER_DELETED,
    INTERNAL_ERROR;

    private final String urn;

    ErrorCode() {
        this.urn = "urn:kartoush:error:" + name().toLowerCase().replace('_', '-');
    }

    public String urn() {
        return urn;
    }
}
