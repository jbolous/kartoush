package com.kartoush.customer.exception;

public class CustomerAddressNotFoundException extends RuntimeException {

    public CustomerAddressNotFoundException(String customerAddressId) {
        super("Address not found for id: " + customerAddressId);
    }
}
