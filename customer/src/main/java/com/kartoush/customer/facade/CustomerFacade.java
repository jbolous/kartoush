package com.kartoush.customer.facade;

import com.kartoush.customer.facade.model.CreateCustomerCommand;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.UpdateCustomerCommand;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.Email;

import java.util.List;

public interface CustomerFacade {

    List<CustomerView> getCustomers();

    CustomerView getCustomer(String customerId);

    CustomerView createCustomer(CreateCustomerCommand command);

    CustomerView updateCustomer(String customerId, UpdateCustomerCommand command);

    CustomerView activateCustomer(String customerId, String rawToken);

    void resendActivationToken(String customerId);

    CustomerView reactivateCustomer(String customerId);

    CustomerView reactivateCustomer(Email email);

    void deleteCustomer(String customerId);

}
