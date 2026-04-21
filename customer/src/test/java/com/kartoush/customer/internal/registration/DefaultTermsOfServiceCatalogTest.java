package com.kartoush.customer.internal.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.persistence.repository.TermsOfServiceRepository;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultTermsOfServiceCatalogTest {

    private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");
    private static final Instant FUTURE_EFFECTIVE_AT = Instant.parse("2026-05-02T00:00:00Z");
    private static final Instant DUE_EFFECTIVE_AT = Instant.parse("2026-05-01T00:00:00Z");

    @Mock
    private TermsOfServiceRepository termsOfServiceRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private DefaultTermsOfServiceCatalog termsOfServiceCatalog;

    @Test
    void shouldReturnCurrentActiveTermsVersion() {
        // given
        final TermsOfServiceEntity activeTerms = TermsOfServiceEntity.rehydrate(
            "01JSVB5F0N3QFJFW4GBB6V2KVS",
            "2026.04.01",
            "Current terms",
            TermsOfServiceContentType.PLAIN_TEXT,
            TermsOfServiceStatus.ACTIVE,
            DUE_EFFECTIVE_AT,
            null,
            DUE_EFFECTIVE_AT,
            DUE_EFFECTIVE_AT
        );

        given(clock.instant()).willReturn(NOW);
        given(termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.ACTIVE)).willReturn(Optional.of(activeTerms));
        given(termsOfServiceRepository.findDueScheduledTermsOfServiceForUpdate(NOW)).willReturn(Optional.empty());

        // when
        final String currentVersion = termsOfServiceCatalog.currentVersion();

        // then
        assertThat(currentVersion).isEqualTo("2026.04.01");
    }

    @Test
    void shouldPromoteDueScheduledTermsWhenResolvingCurrentTerms() {
        // given
        final TermsOfServiceEntity activeTerms = TermsOfServiceEntity.rehydrate(
            "01JSVBHDDWRTR13X79XJX0TQHV",
            "2026.04.01",
            "Current terms",
            TermsOfServiceContentType.PLAIN_TEXT,
            TermsOfServiceStatus.ACTIVE,
            Instant.parse("2026-04-01T00:00:00Z"),
            null,
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z")
        );
        final TermsOfServiceEntity scheduledTerms = TermsOfServiceEntity.rehydrate(
            "01JSVBKEC9M0XSB0YQ0CZPCHKS",
            "2026.05.01",
            "Scheduled terms",
            TermsOfServiceContentType.MARKDOWN,
            TermsOfServiceStatus.SCHEDULED,
            DUE_EFFECTIVE_AT,
            null,
            DUE_EFFECTIVE_AT,
            DUE_EFFECTIVE_AT
        );

        given(clock.instant()).willReturn(NOW);
        given(termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.ACTIVE)).willReturn(Optional.of(activeTerms));
        given(termsOfServiceRepository.findDueScheduledTermsOfServiceForUpdate(NOW)).willReturn(Optional.of(scheduledTerms));
        given(termsOfServiceRepository.save(any(TermsOfServiceEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        final TermsOfServiceEntity currentTerms = termsOfServiceCatalog.currentTerms();

        // then
        assertThat(currentTerms.getVersion()).isEqualTo("2026.05.01");
        assertThat(currentTerms.getStatus()).isEqualTo(TermsOfServiceStatus.ACTIVE);
        assertThat(activeTerms.getStatus()).isEqualTo(TermsOfServiceStatus.SUPERSEDED);
        assertThat(activeTerms.getSupersededAt()).isEqualTo(NOW);
        verify(termsOfServiceRepository).save(activeTerms);
        verify(termsOfServiceRepository).save(scheduledTerms);
    }

    @Test
    void shouldKeepFutureScheduledTermsOutOfCurrentResolution() {
        // given
        final TermsOfServiceEntity activeTerms = TermsOfServiceEntity.rehydrate(
            "01JSVBQMQHV5G0HJGTQ5W64H1M",
            "2026.04.01",
            "Current terms",
            TermsOfServiceContentType.PLAIN_TEXT,
            TermsOfServiceStatus.ACTIVE,
            DUE_EFFECTIVE_AT,
            null,
            DUE_EFFECTIVE_AT,
            DUE_EFFECTIVE_AT
        );

        given(clock.instant()).willReturn(NOW);
        given(termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.ACTIVE)).willReturn(Optional.of(activeTerms));
        given(termsOfServiceRepository.findDueScheduledTermsOfServiceForUpdate(NOW)).willReturn(Optional.empty());

        // when
        final TermsOfServiceEntity currentTerms = termsOfServiceCatalog.currentTerms();

        // then
        assertThat(currentTerms.getVersion()).isEqualTo("2026.04.01");
        assertThat(currentTerms.getStatus()).isEqualTo(TermsOfServiceStatus.ACTIVE);
    }

    @Test
    void shouldThrowWhenNoActiveTermsAreConfigured() {
        // given
        given(clock.instant()).willReturn(NOW);
        given(termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.ACTIVE)).willReturn(Optional.empty());
        given(termsOfServiceRepository.findDueScheduledTermsOfServiceForUpdate(NOW)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> termsOfServiceCatalog.currentTerms())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No active Terms of Service configured");
    }
}
