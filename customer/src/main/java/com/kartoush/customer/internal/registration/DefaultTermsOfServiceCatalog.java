package com.kartoush.customer.internal.registration;

import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.persistence.repository.TermsOfServiceRepository;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultTermsOfServiceCatalog implements TermsOfServiceCatalog {

    private final TermsOfServiceRepository termsOfServiceRepository;
    private final Clock clock;

    public DefaultTermsOfServiceCatalog(
        final TermsOfServiceRepository termsOfServiceRepository,
        final Clock clock) {
        this.termsOfServiceRepository = termsOfServiceRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public String currentVersion() {
        return currentTerms().getVersion();
    }

    @Override
    @Transactional
    public TermsOfServiceEntity currentTerms() {
        final Instant now = Instant.now(clock);
        final Optional<TermsOfServiceEntity> activeTermsOfService =
            termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.ACTIVE);
        final Optional<TermsOfServiceEntity> dueScheduledTermsOfService =
            termsOfServiceRepository.findDueScheduledTermsOfServiceForUpdate(now);

        if (dueScheduledTermsOfService.isPresent()) {
            final TermsOfServiceEntity scheduledTermsOfService = dueScheduledTermsOfService.get();
            // Lock both the ACTIVE and due SCHEDULED Terms rows before promotion so only one
            // transaction can supersede and activate them, keeping lifecycle timestamps stable.
            activeTermsOfService.ifPresent(active -> {
                if (!Objects.equals(active.getId(), scheduledTermsOfService.getId())) {
                    active.supersede(now);
                    termsOfServiceRepository.save(active);
                }
            });

            scheduledTermsOfService.activate(scheduledTermsOfService.getEffectiveAt());
            return termsOfServiceRepository.save(scheduledTermsOfService);
        }

        return activeTermsOfService.orElseThrow(
            () -> new IllegalStateException("No active Terms of Service configured"));
    }
}
