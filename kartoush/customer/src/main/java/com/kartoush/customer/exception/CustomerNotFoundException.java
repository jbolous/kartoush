package com.kartoush.customer.exception;

public class CustomerNotFoundException extends RuntimeException
{
    public CustomerNotFoundException(final String customerId)
    {
        super("Customer not found for id: " + customerId);
    }
}
