package com.kartoush.customer.service;

import com.kartoush.customer.domain.Customer;
import com.kartoush.customer.domain.CustomerProfile;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;

import java.util.List;
import java.util.Optional;

public interface CustomerService
{
    List<Customer> getActiveCustomers();

    Optional<Customer> getCustomerById(String customerId);

    Optional<Customer> findCustomerByEmail(Email email);

    Customer updateCustomer(final String customerId, final CustomerProfile profile);

    Customer registerCustomer(final Customer customer, final String termsVersion);

    Customer activateCustomer(final String customerId, final String rawToken);

    ActivationEmailDelivery issueActivationTokenForResend(final String customerId);

    Customer reactivateCustomer(final String customerId);

    void deleteCustomer(CustomerId customerId);

    Customer reactivateCustomerByEmail(Email email);
}
