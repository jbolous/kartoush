package com.kartoush.api.dev;

import com.kartoush.api.auth.PasswordResetService;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CustomerActivationView;
import com.kartoush.platform.types.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DevEmailActionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@ImportAutoConfiguration(exclude = {
    SpringDocConfiguration.class,
    SpringDocSpecPropertiesConfiguration.class,
    SpringDocWebMvcConfiguration.class,
    MultipleOpenApiSupportConfiguration.class
})
class DevEmailActionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiProblemFactory apiProblemFactory;

    @MockitoBean
    private CustomerFacade customerFacade;

    @MockitoBean
    private PasswordResetService customerPasswordResetApplicationService;

    @Test
    void shouldRenderActivationPage() throws Exception {
        mockMvc.perform(get("/dev/customers/activate")
                .param("customerId", "customer-123")
                .param("token", "activation-token"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Activate customer")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("activation-token")));
    }

    @Test
    void shouldActivateCustomerAndRenderInitialPasswordForm() throws Exception {
        when(customerFacade.activateCustomer("customer-123", "activation-token")).thenReturn(
            new CustomerActivationView(
                "customer-123",
                "Jack",
                "Kartoush",
                "jack@kartoush.dev",
                "",
                CustomerStatus.ACTIVE,
                "setup-token"
            )
        );

        mockMvc.perform(post("/dev/customers/activate")
                .param("customerId", "customer-123")
                .param("token", "activation-token"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Customer activated")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("setup-token")));
    }

    @Test
    void shouldResetPasswordFromDevPage() throws Exception {
        mockMvc.perform(post("/dev/auth/password-reset")
                .param("email", "jack@kartoush.dev")
                .param("token", "reset-token")
                .param("password", "Password123!")
                .param("confirmPassword", "Password123!"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Password reset complete")));

        verify(customerPasswordResetApplicationService).resetPassword(any());
    }
}
