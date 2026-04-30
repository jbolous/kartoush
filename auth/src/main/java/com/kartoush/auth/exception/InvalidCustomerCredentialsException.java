package com.kartoush.auth.exception;

public class InvalidCustomerCredentialsException extends RuntimeException {

    public InvalidCustomerCredentialsException() {
        super("Customer authentication failed");
    }
}
