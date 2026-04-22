package com.kartoush.customer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.kartoush.customer.exception.InvalidTermsOfServiceScheduleException;
import com.kartoush.customer.exception.InvalidTermsOfServiceTransitionException;
import com.kartoush.customer.exception.NoDueScheduledTermsOfServiceException;
import com.kartoush.customer.exception.TermsOfServiceNotFoundException;
import com.kartoush.customer.exception.TermsOfServiceVersionAlreadyExistsException;
import com.kartoush.customer.exception.TermsOfServiceVersionAlreadyScheduledException;
import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.persistence.repository.TermsOfServiceRepository;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import com.kartoush.platform.ulid.UlidGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class DefaultTermsOfServiceManagementServiceTest {

    private static final String TERMS_ID = "01JV00MANAGEDTERMS00000001";
    private static final String ACTIVE_TERMS_ID = "01JV00MANAGEDTERMS00000002";
    private static final String SCHEDULED_TERMS_ID = "01JV00MANAGEDTERMS00000003";
    private static final String VERSION = "2026.06.01";
    private static final String VERSION_WITH_WHITESPACE = "   2026.06.01   ";
    private static final String ACTIVE_VERSION = "2026.04.01";
    private static final String CONTENT = "Draft terms";
    private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");
    private static final Instant FUTURE_EFFECTIVE_AT = Instant.parse("2026-05-10T00:00:00Z");
    private static final Instant DUE_EFFECTIVE_AT = Instant.parse("2026-05-01T00:00:00Z");

    @Mock
    private TermsOfServiceRepository termsOfServiceRepository;

    @Mock
    private UlidGenerator ulidGenerator;

    @Mock
    private Clock clock;

    @InjectMocks
    private DefaultTermsOfServiceManagementService termsOfServiceManagementService;

    @Test
    void shouldCreateDraftTermsOfService() {
        // given
        given(ulidGenerator.next()).willReturn(TERMS_ID);
        given(termsOfServiceRepository.findByVersion(VERSION)).willReturn(Optional.empty());
        given(termsOfServiceRepository.save(any(TermsOfServiceEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        final TermsOfServiceEntity terms = termsOfServiceManagementService.createDraft(
            VERSION,
            CONTENT,
            TermsOfServiceContentType.MARKDOWN
        );

        // then
        assertThat(terms.getId()).isEqualTo(TERMS_ID);
        assertThat(terms.getVersion()).isEqualTo(VERSION);
        assertThat(terms.getStatus()).isEqualTo(TermsOfServiceStatus.DRAFT);
        verify(termsOfServiceRepository).save(any(TermsOfServiceEntity.class));
    }

    @Test
    void shouldRejectDuplicateDraftVersion() {
        // given
        final TermsOfServiceEntity existingTerms = TermsOfServiceEntity.draft(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT
        );
        given(termsOfServiceRepository.findByVersion(VERSION)).willReturn(Optional.of(existingTerms));

        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.createDraft(
            VERSION,
            CONTENT,
            TermsOfServiceContentType.MARKDOWN
        ))
            .isInstanceOf(TermsOfServiceVersionAlreadyExistsException.class)
            .hasMessage("Terms of Service already exists for version: " + VERSION);

        verify(termsOfServiceRepository, never()).save(any());
    }

    @Test
    void shouldUpdateDraftContent() {
        // given
        final TermsOfServiceEntity draftTerms = TermsOfServiceEntity.draft(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT
        );
        given(termsOfServiceRepository.findByIdForUpdate(TERMS_ID)).willReturn(Optional.of(draftTerms));
        given(termsOfServiceRepository.save(draftTerms)).willReturn(draftTerms);

        // when
        final TermsOfServiceEntity updatedTerms = termsOfServiceManagementService.updateDraft(
            TERMS_ID,
            "Updated terms",
            TermsOfServiceContentType.MARKDOWN
        );

        // then
        assertThat(updatedTerms.getContent()).isEqualTo("Updated terms");
        assertThat(updatedTerms.getContentType()).isEqualTo(TermsOfServiceContentType.MARKDOWN);
        verify(termsOfServiceRepository).save(draftTerms);
    }

    @Test
    void shouldRejectDraftUpdateWhenTermsAreNotDraft() {
        // given
        final TermsOfServiceEntity activeTerms = activeTerms(ACTIVE_TERMS_ID, ACTIVE_VERSION);
        given(termsOfServiceRepository.findByIdForUpdate(ACTIVE_TERMS_ID)).willReturn(Optional.of(activeTerms));

        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.updateDraft(
            ACTIVE_TERMS_ID,
            "Updated terms",
            TermsOfServiceContentType.MARKDOWN
        ))
            .isInstanceOf(InvalidTermsOfServiceTransitionException.class)
            .hasMessage("Terms of Service content can only be modified while in DRAFT");

        verify(termsOfServiceRepository, never()).save(any());
    }

    @Test
    void shouldScheduleDraftTermsOfService() {
        // given
        final TermsOfServiceEntity draftTerms = TermsOfServiceEntity.draft(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT
        );
        given(clock.instant()).willReturn(NOW);
        given(termsOfServiceRepository.findByIdForUpdate(TERMS_ID)).willReturn(Optional.of(draftTerms));
        given(termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.SCHEDULED)).willReturn(Optional.empty());
        given(termsOfServiceRepository.save(draftTerms)).willReturn(draftTerms);

        // when
        final TermsOfServiceEntity scheduledTerms =
            termsOfServiceManagementService.schedule(TERMS_ID, FUTURE_EFFECTIVE_AT);

        // then
        assertThat(scheduledTerms.getStatus()).isEqualTo(TermsOfServiceStatus.SCHEDULED);
        assertThat(scheduledTerms.getEffectiveAt()).isEqualTo(FUTURE_EFFECTIVE_AT);
    }

    @Test
    void shouldRejectScheduleWhenEffectiveAtIsNotFuture() {
        // given
        given(clock.instant()).willReturn(NOW);
        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.schedule(TERMS_ID, NOW))
            .isInstanceOf(InvalidTermsOfServiceScheduleException.class)
            .hasMessage("Terms of Service can only be scheduled for a future effectiveAt: " + NOW);
    }

    @Test
    void shouldRejectScheduleWhenAnotherTermsVersionIsAlreadyScheduled() {
        // given
        final TermsOfServiceEntity draftTerms = TermsOfServiceEntity.draft(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT
        );
        final TermsOfServiceEntity scheduledTerms = TermsOfServiceEntity.rehydrate(
            SCHEDULED_TERMS_ID,
            "2026.07.01",
            "Scheduled",
            TermsOfServiceContentType.PLAIN_TEXT,
            TermsOfServiceStatus.SCHEDULED,
            FUTURE_EFFECTIVE_AT,
            null,
            NOW,
            NOW
        );
        given(clock.instant()).willReturn(NOW);
        given(termsOfServiceRepository.findByIdForUpdate(TERMS_ID)).willReturn(Optional.of(draftTerms));
        given(termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.SCHEDULED))
            .willReturn(Optional.of(scheduledTerms));

        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.schedule(TERMS_ID, FUTURE_EFFECTIVE_AT))
            .isInstanceOf(TermsOfServiceVersionAlreadyScheduledException.class)
            .hasMessage("Another Terms of Service version is already scheduled: " + SCHEDULED_TERMS_ID);

        verify(termsOfServiceRepository, never()).save(any());
    }

    @Test
    void shouldUnscheduleScheduledTermsOfService() {
        // given
        final TermsOfServiceEntity scheduledTerms = TermsOfServiceEntity.rehydrate(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT,
            TermsOfServiceStatus.SCHEDULED,
            FUTURE_EFFECTIVE_AT,
            null,
            NOW,
            NOW
        );
        given(termsOfServiceRepository.findByIdForUpdate(TERMS_ID)).willReturn(Optional.of(scheduledTerms));
        given(termsOfServiceRepository.save(scheduledTerms)).willReturn(scheduledTerms);

        // when
        final TermsOfServiceEntity unscheduledTerms = termsOfServiceManagementService.unschedule(TERMS_ID);

        // then
        assertThat(unscheduledTerms.getStatus()).isEqualTo(TermsOfServiceStatus.DRAFT);
        assertThat(unscheduledTerms.getEffectiveAt()).isNull();
    }

    @Test
    void shouldRejectUnscheduleWhenTermsAreNotScheduled() {
        // given
        final TermsOfServiceEntity draftTerms = TermsOfServiceEntity.draft(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT
        );
        given(termsOfServiceRepository.findByIdForUpdate(TERMS_ID)).willReturn(Optional.of(draftTerms));

        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.unschedule(TERMS_ID))
            .isInstanceOf(InvalidTermsOfServiceTransitionException.class)
            .hasMessage("Terms of Service can only be unscheduled from SCHEDULED");
    }

    @Test
    void shouldActivateDraftTermsImmediatelyAndSupersedeCurrentActiveTerms() {
        // given
        final TermsOfServiceEntity draftTerms = TermsOfServiceEntity.draft(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.MARKDOWN
        );
        final TermsOfServiceEntity activeTerms = activeTerms(ACTIVE_TERMS_ID, ACTIVE_VERSION);
        given(clock.instant()).willReturn(NOW);
        given(termsOfServiceRepository.findByIdForUpdate(TERMS_ID)).willReturn(Optional.of(draftTerms));
        given(termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.ACTIVE)).willReturn(Optional.of(activeTerms));
        given(termsOfServiceRepository.save(any(TermsOfServiceEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        final TermsOfServiceEntity activatedTerms = termsOfServiceManagementService.activateNow(TERMS_ID);

        // then
        assertThat(activatedTerms.getStatus()).isEqualTo(TermsOfServiceStatus.ACTIVE);
        assertThat(activatedTerms.getEffectiveAt()).isEqualTo(NOW);
        assertThat(activeTerms.getStatus()).isEqualTo(TermsOfServiceStatus.SUPERSEDED);
        assertThat(activeTerms.getSupersededAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRejectImmediateActivationWhenTermsCannotTransitionToActive() {
        // given
        final TermsOfServiceEntity supersededTerms = TermsOfServiceEntity.rehydrate(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.MARKDOWN,
            TermsOfServiceStatus.SUPERSEDED,
            DUE_EFFECTIVE_AT,
            NOW,
            DUE_EFFECTIVE_AT,
            NOW
        );
        given(clock.instant()).willReturn(NOW);
        given(termsOfServiceRepository.findByIdForUpdate(TERMS_ID)).willReturn(Optional.of(supersededTerms));
        given(termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.ACTIVE)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.activateNow(TERMS_ID))
            .isInstanceOf(InvalidTermsOfServiceTransitionException.class)
            .hasMessage("Terms of Service can only be activated from DRAFT or SCHEDULED");
    }

    @Test
    void shouldPromoteDueScheduledTerms() {
        // given
        final TermsOfServiceEntity dueScheduledTerms = TermsOfServiceEntity.rehydrate(
            SCHEDULED_TERMS_ID,
            "2026.07.01",
            "Scheduled",
            TermsOfServiceContentType.PLAIN_TEXT,
            TermsOfServiceStatus.SCHEDULED,
            DUE_EFFECTIVE_AT,
            null,
            DUE_EFFECTIVE_AT,
            DUE_EFFECTIVE_AT
        );
        final TermsOfServiceEntity activeTerms = activeTerms(ACTIVE_TERMS_ID, ACTIVE_VERSION);
        given(clock.instant()).willReturn(NOW);
        given(termsOfServiceRepository.findDueScheduledTermsOfServiceForUpdate(NOW))
            .willReturn(Optional.of(dueScheduledTerms));
        given(termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.ACTIVE)).willReturn(Optional.of(activeTerms));
        given(termsOfServiceRepository.save(any(TermsOfServiceEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        final TermsOfServiceEntity promotedTerms = termsOfServiceManagementService.promoteDueScheduledTerms();

        // then
        assertThat(promotedTerms.getStatus()).isEqualTo(TermsOfServiceStatus.ACTIVE);
        assertThat(promotedTerms.getEffectiveAt()).isEqualTo(DUE_EFFECTIVE_AT);
        assertThat(activeTerms.getStatus()).isEqualTo(TermsOfServiceStatus.SUPERSEDED);
        assertThat(activeTerms.getSupersededAt()).isEqualTo(NOW);
    }

    @Test
    void shouldThrowWhenPromotingWithoutDueScheduledTerms() {
        // given
        given(clock.instant()).willReturn(NOW);
        given(termsOfServiceRepository.findDueScheduledTermsOfServiceForUpdate(NOW)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.promoteDueScheduledTerms())
            .isInstanceOf(NoDueScheduledTermsOfServiceException.class)
            .hasMessage("No due scheduled Terms of Service found");
    }

    @Test
    void shouldThrowWhenTermsOfServiceIsMissing() {
        // given
        given(termsOfServiceRepository.findByIdForUpdate(TERMS_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.updateDraft(
            TERMS_ID,
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT
        ))
            .isInstanceOf(TermsOfServiceNotFoundException.class)
            .hasMessage("Terms of Service not found for id: " + TERMS_ID);
    }

    @Test
    void shouldThrowWhenSchedulingMissingTermsOfService() {
        // given
        given(clock.instant()).willReturn(NOW);
        given(termsOfServiceRepository.findByIdForUpdate(TERMS_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.schedule(TERMS_ID, FUTURE_EFFECTIVE_AT))
            .isInstanceOf(TermsOfServiceNotFoundException.class)
            .hasMessage("Terms of Service not found for id: " + TERMS_ID);
    }

    @Test
    void shouldThrowWhenUnschedulingMissingTermsOfService() {
        // given
        given(termsOfServiceRepository.findByIdForUpdate(TERMS_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.unschedule(TERMS_ID))
            .isInstanceOf(TermsOfServiceNotFoundException.class)
            .hasMessage("Terms of Service not found for id: " + TERMS_ID);
    }

    @Test
    void shouldThrowWhenActivatingMissingTermsOfService() {
        // given
        given(clock.instant()).willReturn(NOW);
        given(termsOfServiceRepository.findByIdForUpdate(TERMS_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.activateNow(TERMS_ID))
            .isInstanceOf(TermsOfServiceNotFoundException.class)
            .hasMessage("Terms of Service not found for id: " + TERMS_ID);
    }

    @Test
    void shouldRejectImmediateActivationWhenTermsAreAlreadyActive() {
        // given
        final TermsOfServiceEntity activeTerms = activeTerms(TERMS_ID, VERSION);
        given(clock.instant()).willReturn(NOW);
        given(termsOfServiceRepository.findByIdForUpdate(TERMS_ID)).willReturn(Optional.of(activeTerms));
        given(termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.ACTIVE)).willReturn(Optional.of(activeTerms));

        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.activateNow(TERMS_ID))
            .isInstanceOf(InvalidTermsOfServiceTransitionException.class)
            .hasMessage("Terms of Service can only be activated from DRAFT or SCHEDULED");
    }

    @Test
    void shouldRejectDuplicateDraftVersionAfterNormalizingWhitespace() {
        // given
        final TermsOfServiceEntity existingTerms = TermsOfServiceEntity.draft(
            TERMS_ID,
            VERSION,
            CONTENT,
            TermsOfServiceContentType.PLAIN_TEXT
        );
        given(termsOfServiceRepository.findByVersion(VERSION)).willReturn(Optional.of(existingTerms));

        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.createDraft(
            VERSION_WITH_WHITESPACE,
            CONTENT,
            TermsOfServiceContentType.MARKDOWN
        ))
            .isInstanceOf(TermsOfServiceVersionAlreadyExistsException.class)
            .hasMessage("Terms of Service already exists for version: " + VERSION);

        verify(termsOfServiceRepository).findByVersion(VERSION);
        verify(termsOfServiceRepository, never()).save(any());
    }

    @Test
    void shouldTranslateUniqueConstraintViolationToDuplicateDraftVersion() {
        // given
        given(ulidGenerator.next()).willReturn(TERMS_ID);
        given(termsOfServiceRepository.findByVersion(VERSION)).willReturn(Optional.empty());
        given(termsOfServiceRepository.save(any(TermsOfServiceEntity.class)))
            .willThrow(new DataIntegrityViolationException("unique constraint violation"));

        // when / then
        assertThatThrownBy(() -> termsOfServiceManagementService.createDraft(
            VERSION_WITH_WHITESPACE,
            CONTENT,
            TermsOfServiceContentType.MARKDOWN
        ))
            .isInstanceOf(TermsOfServiceVersionAlreadyExistsException.class)
            .hasMessage("Terms of Service already exists for version: " + VERSION);

        verify(termsOfServiceRepository).findByVersion(VERSION);
        verify(termsOfServiceRepository).save(any(TermsOfServiceEntity.class));
    }

    @Test
    void shouldCreateDraftTermsOfServiceUsingNormalizedVersion() {
        // given
        given(ulidGenerator.next()).willReturn(TERMS_ID);
        given(termsOfServiceRepository.findByVersion(VERSION)).willReturn(Optional.empty());
        given(termsOfServiceRepository.save(any(TermsOfServiceEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        final TermsOfServiceEntity terms = termsOfServiceManagementService.createDraft(
            VERSION_WITH_WHITESPACE,
            CONTENT,
            TermsOfServiceContentType.MARKDOWN
        );

        // then
        assertThat(terms.getId()).isEqualTo(TERMS_ID);
        assertThat(terms.getVersion()).isEqualTo(VERSION);
        assertThat(terms.getStatus()).isEqualTo(TermsOfServiceStatus.DRAFT);
        verify(termsOfServiceRepository).findByVersion(VERSION);
        verify(termsOfServiceRepository).save(any(TermsOfServiceEntity.class));
    }

    private static TermsOfServiceEntity activeTerms(final String id, final String version) {
        return TermsOfServiceEntity.rehydrate(
            id,
            version,
            "Active terms",
            TermsOfServiceContentType.PLAIN_TEXT,
            TermsOfServiceStatus.ACTIVE,
            DUE_EFFECTIVE_AT,
            null,
            DUE_EFFECTIVE_AT,
            DUE_EFFECTIVE_AT
        );
    }
}
