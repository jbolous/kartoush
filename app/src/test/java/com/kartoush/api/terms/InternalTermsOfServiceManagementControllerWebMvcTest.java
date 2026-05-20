package com.kartoush.api.terms;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartoush.api.error.ApiProblemFactory;
import com.kartoush.api.error.ErrorCode;
import com.kartoush.customer.exception.InvalidTermsOfServiceScheduleException;
import com.kartoush.customer.exception.TermsOfServiceNotFoundException;
import com.kartoush.customer.facade.TermsOfServiceFacade;
import com.kartoush.customer.exception.TermsOfServiceVersionAlreadyExistsException;
import com.kartoush.customer.facade.TermsOfServiceManagementFacade;
import com.kartoush.customer.facade.model.TermsOfServiceManagementView;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternalTermsOfServiceManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
    SpringDocConfiguration.class,
    SpringDocSpecPropertiesConfiguration.class,
    SpringDocWebMvcConfiguration.class,
    MultipleOpenApiSupportConfiguration.class
})
class InternalTermsOfServiceManagementControllerWebMvcTest {

    private static final String BASE_URL = "/internal/terms-of-service";
    private static final String TERMS_ID = "01KQ0INTERNALTERMS000000001";
    private static final String VERSION = "2026.05.01";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ApiProblemFactory apiProblemFactory;

    @MockitoBean
    private TermsOfServiceManagementFacade termsOfServiceManagementFacade;

    @MockitoBean
    private TermsOfServiceFacade termsOfServiceFacade;

    @Test
    void shouldCreateDraft() throws Exception {
        final CreateTermsOfServiceDraftRequest request = new CreateTermsOfServiceDraftRequest(
            VERSION,
            "Draft content",
            TermsOfServiceContentType.MARKDOWN
        );

        when(termsOfServiceManagementFacade.createDraft(
            request.version(),
            request.content(),
            request.contentType()
        )).thenReturn(managementView(TermsOfServiceStatus.DRAFT));

        mockMvc.perform(post(BASE_URL + "/drafts")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/internal/terms-of-service/drafts/" + TERMS_ID))
            .andExpect(jsonPath("$.id").value(TERMS_ID))
            .andExpect(jsonPath("$.status").value(TermsOfServiceStatus.DRAFT.name()));

        verify(termsOfServiceManagementFacade).createDraft(
            request.version(),
            request.content(),
            request.contentType()
        );
    }

    @Test
    void shouldUpdateDraft() throws Exception {
        final UpdateTermsOfServiceDraftRequest request = new UpdateTermsOfServiceDraftRequest(
            "Updated draft content",
            TermsOfServiceContentType.PLAIN_TEXT
        );

        when(termsOfServiceManagementFacade.updateDraft(TERMS_ID, request.content(), request.contentType()))
            .thenReturn(managementView(TermsOfServiceStatus.DRAFT));

        mockMvc.perform(put(BASE_URL + "/drafts/{termsOfServiceId}", TERMS_ID)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TERMS_ID));
    }

    @Test
    void shouldScheduleTerms() throws Exception {
        final ScheduleTermsOfServiceRequest request = new ScheduleTermsOfServiceRequest(
            Instant.parse("2026-05-01T00:00:00Z")
        );

        when(termsOfServiceManagementFacade.schedule(TERMS_ID, request.effectiveAt()))
            .thenReturn(managementView(TermsOfServiceStatus.SCHEDULED));

        mockMvc.perform(post(BASE_URL + "/{termsOfServiceId}/schedule", TERMS_ID)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(TermsOfServiceStatus.SCHEDULED.name()));
    }

    @Test
    void shouldPromoteDueScheduledTerms() throws Exception {
        when(termsOfServiceManagementFacade.promoteDueScheduledTerms())
            .thenReturn(managementView(TermsOfServiceStatus.ACTIVE));

        mockMvc.perform(post(BASE_URL + "/promote-due"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(TermsOfServiceStatus.ACTIVE.name()));
    }

    @Test
    void shouldReturnConflictForDuplicateTermsVersion() throws Exception {
        final CreateTermsOfServiceDraftRequest request = new CreateTermsOfServiceDraftRequest(
            VERSION,
            "Draft content",
            TermsOfServiceContentType.MARKDOWN
        );
        final ProblemDetail problemDetail = problemDetail(
            HttpStatus.CONFLICT,
            "Terms of Service Already Exists",
            ErrorCode.TERMS_OF_SERVICE_ALREADY_EXISTS
        );

        when(termsOfServiceManagementFacade.createDraft(
            request.version(),
            request.content(),
            request.contentType()
        ))
            .thenThrow(new TermsOfServiceVersionAlreadyExistsException(VERSION));
        when(apiProblemFactory.create(any(), any(), any(), any(), any())).thenReturn(problemDetail);

        mockMvc.perform(post(BASE_URL + "/drafts")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.TERMS_OF_SERVICE_ALREADY_EXISTS.name()));
    }

    @Test
    void shouldReturnBadRequestForInvalidSchedule() throws Exception {
        final ScheduleTermsOfServiceRequest request = new ScheduleTermsOfServiceRequest(
            Instant.parse("2026-04-01T00:00:00Z")
        );
        final ProblemDetail problemDetail = problemDetail(
            HttpStatus.BAD_REQUEST,
            "Invalid Terms of Service Schedule",
            ErrorCode.INVALID_TERMS_OF_SERVICE_SCHEDULE
        );

        when(termsOfServiceManagementFacade.schedule(TERMS_ID, request.effectiveAt()))
            .thenThrow(new InvalidTermsOfServiceScheduleException(request.effectiveAt()));
        when(apiProblemFactory.create(any(), any(), any(), any(), any())).thenReturn(problemDetail);

        mockMvc.perform(post(BASE_URL + "/{termsOfServiceId}/schedule", TERMS_ID)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_TERMS_OF_SERVICE_SCHEDULE.name()));
    }

    @Test
    void shouldReturnNotFoundForMissingTerms() throws Exception {
        final ProblemDetail problemDetail = problemDetail(
            HttpStatus.NOT_FOUND,
            "Terms of Service Not Found",
            ErrorCode.TERMS_OF_SERVICE_NOT_FOUND
        );

        when(termsOfServiceManagementFacade.unschedule(TERMS_ID))
            .thenThrow(new TermsOfServiceNotFoundException(TERMS_ID));
        when(apiProblemFactory.create(any(), any(), any(), any(), any())).thenReturn(problemDetail);

        mockMvc.perform(post(BASE_URL + "/{termsOfServiceId}/unschedule", TERMS_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.TERMS_OF_SERVICE_NOT_FOUND.name()));
    }

    private TermsOfServiceManagementView managementView(final TermsOfServiceStatus status) {
        return new TermsOfServiceManagementView(
            TERMS_ID,
            VERSION,
            "Terms content",
            TermsOfServiceContentType.MARKDOWN,
            status,
            Instant.parse("2026-05-01T00:00:00Z"),
            null,
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z")
        );
    }

    private ProblemDetail problemDetail(
        final HttpStatus status,
        final String title,
        final ErrorCode errorCode
    ) {
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, title);
        problemDetail.setTitle(title);
        problemDetail.setProperty("errorCode", errorCode.name());
        return problemDetail;
    }
}
