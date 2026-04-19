package com.kartoush.api.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.api.error.ErrorCode;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CreateCustomerRequest;
import com.kartoush.customer.facade.model.CustomerView;
import com.kartoush.customer.facade.model.UpdateCustomerRequest;
import com.kartoush.platform.types.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

    private static final String TERMS_VERSION = "2026-04";

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
            PHONE,
            true,
            TERMS_VERSION
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

    @Test
    void shouldActivateCustomer() throws Exception {
        final ActivateCustomerRequest request = new ActivateCustomerRequest("valid-activation-token");

        when(customerFacade.activateCustomer(eq(CUSTOMER_ID), eq(request.token())))
            .thenReturn(mockCustomerView());

        mockMvc.perform(post(BASE_URL + "/{customerId}/activation", CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID));

        verify(customerFacade).activateCustomer(eq(CUSTOMER_ID), eq(request.token()));
    }

    @Test
    void shouldReturnBadRequestWhenActivationTokenIsBlank() throws Exception {
        final ActivateCustomerRequest request = new ActivateCustomerRequest("   ");
        final ProblemDetail validationProblem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "One or more validation errors occurred.");
        validationProblem.setTitle("Validation Failed");
        validationProblem.setProperty("errorCode", ErrorCode.VALIDATION_FAILED.name());
        validationProblem.setProperty("errors", List.of());

        when(apiProblemFactory.create(
            eq(HttpStatus.BAD_REQUEST),
            eq("Validation Failed"),
            eq("One or more validation errors occurred."),
            eq(ErrorCode.VALIDATION_FAILED),
            any())).thenReturn(validationProblem);

        mockMvc.perform(post(BASE_URL + "/{customerId}/activation", CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldResendActivationToken() throws Exception {
        mockMvc.perform(post(BASE_URL + "/{customerId}/activation/resend", CUSTOMER_ID))
            .andExpect(status().isNoContent());

        verify(customerFacade).resendActivationToken(eq(CUSTOMER_ID));
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
