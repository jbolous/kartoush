package com.kartoush.api.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.UpdateCustomerRequest;
import com.kartoush.platform.types.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerWebMvcTest {
    private static final String BASE_URL = "/api/customers";

    private static final String CUSTOMER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    private static final String FIRST_NAME = "Jack";

    private static final String LAST_NAME = "Kartoush";

    private static final String EMAIL = "jack@kartoush.com";

    private static final String PHONE = "+16305551234";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ApiProblemFactory apiProblemFactory;

    @MockitoBean
    private CustomerFacade customerFacade;

    @Test
    void shouldGetCustomers() throws Exception {
        when(customerFacade.getCustomers()).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isOk());

        verify(customerFacade).getCustomers();
    }

    @Test
    void shouldGetCustomerById() throws Exception {
        when(customerFacade.getCustomer(eq(CUSTOMER_ID)))
            .thenReturn(mockCustomerView());

        mockMvc.perform(get(BASE_URL + "/{customerId}", CUSTOMER_ID))
            .andExpect(status().isOk());

        verify(customerFacade).getCustomer(eq(CUSTOMER_ID));
    }

    @Test
    void shouldCreateCustomer() throws Exception {
        final CreateCustomerRequest request = new CreateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            EMAIL,
            PHONE
        );

        when(customerFacade.createCustomer(any()))
            .thenReturn(mockCustomerView());

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", BASE_URL + "/" + CUSTOMER_ID))
            .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID))
            .andExpect(jsonPath("$.email").value(EMAIL))
            .andExpect(jsonPath("$.firstName").value(FIRST_NAME))
            .andExpect(jsonPath("$.lastName").value(LAST_NAME));

        verify(customerFacade).createCustomer(any());
    }

    @Test
    void shouldUpdateCustomer() throws Exception {
        final UpdateCustomerRequest request = new UpdateCustomerRequest(
            FIRST_NAME,
            LAST_NAME,
            PHONE
        );

        when(customerFacade.updateCustomer(eq(CUSTOMER_ID), any(UpdateCustomerRequest.class)))
            .thenReturn(mockCustomerView());

        mockMvc.perform(put(BASE_URL + "/{customerId}", CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(customerFacade).updateCustomer(eq(CUSTOMER_ID), any(UpdateCustomerRequest.class));
    }

    @Test
    void shouldDeleteCustomer() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{customerId}", CUSTOMER_ID))
            .andExpect(status().isNoContent());

        verify(customerFacade).deleteCustomer(eq(CUSTOMER_ID));
    }

    private CustomerView mockCustomerView() {
        return new CustomerView(
            CUSTOMER_ID,
            FIRST_NAME,
            LAST_NAME,
            EMAIL,
            PHONE,
            CustomerStatus.ACTIVE);
    }
}
