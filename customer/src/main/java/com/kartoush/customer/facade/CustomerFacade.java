package com.kartoush.customer.facade;

import com.kartoush.customer.facade.model.CreateCustomerInput;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.UpdateCustomerInput;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;

import java.util.List;

public interface CustomerFacade {

    List<CustomerView> getCustomers();

    CustomerView getCustomer(String customerId);

    CustomerView createCustomer(CreateCustomerInput input);

    CustomerView updateCustomer(String customerId, UpdateCustomerInput input);

    CustomerView activateCustomer(String customerId, String rawToken);

    void resendActivationToken(String customerId);

    CustomerView reactivateCustomer(String customerId);

    CustomerView reactivateCustomer(Email email);

    void deleteCustomer(String customerId);

}
