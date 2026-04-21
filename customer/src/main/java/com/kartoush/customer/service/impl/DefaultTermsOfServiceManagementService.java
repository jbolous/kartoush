package com.kartoush.customer.service.impl;

import com.kartoush.customer.exception.InvalidTermsOfServiceScheduleException;
import com.kartoush.customer.exception.InvalidTermsOfServiceTransitionException;
import com.kartoush.customer.exception.NoDueScheduledTermsOfServiceException;
import com.kartoush.customer.exception.TermsOfServiceNotFoundException;
import com.kartoush.customer.exception.TermsOfServiceVersionAlreadyExistsException;
import com.kartoush.customer.exception.TermsOfServiceVersionAlreadyScheduledException;
import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.persistence.repository.TermsOfServiceRepository;
import com.kartoush.customer.service.TermsOfServiceManagementService;
import com.kartoush.customer.termsofservice.TermsOfServiceContentType;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import com.kartoush.platform.ulid.UlidGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultTermsOfServiceManagementService implements TermsOfServiceManagementService {

    private final TermsOfServiceRepository termsOfServiceRepository;
    private final UlidGenerator ulidGenerator;
    private final Clock clock;

    public DefaultTermsOfServiceManagementService(
        final TermsOfServiceRepository termsOfServiceRepository,
        final UlidGenerator ulidGenerator,
        final Clock clock) {
        this.termsOfServiceRepository = termsOfServiceRepository;
        this.ulidGenerator = ulidGenerator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TermsOfServiceEntity createDraft(
        final String version,
        final String content,
        final TermsOfServiceContentType contentType) {
        if (termsOfServiceRepository.findByVersion(version).isPresent()) {
            throw new TermsOfServiceVersionAlreadyExistsException(version);
        }

        final TermsOfServiceEntity termsOfService = TermsOfServiceEntity.draft(
            ulidGenerator.next(),
            version,
            content,
            contentType
        );

        return termsOfServiceRepository.save(termsOfService);
    }

    @Override
    @Transactional
    public TermsOfServiceEntity updateDraft(
        final String termsOfServiceId,
        final String content,
        final TermsOfServiceContentType contentType) {
        final TermsOfServiceEntity termsOfService = findByIdForUpdate(termsOfServiceId);

        try {
            termsOfService.updateDraftContent(content, contentType);
        } catch (final IllegalStateException exception) {
            throw new InvalidTermsOfServiceTransitionException(exception.getMessage());
        }

        return termsOfServiceRepository.save(termsOfService);
    }

    @Override
    @Transactional
    public TermsOfServiceEntity schedule(
        final String termsOfServiceId,
        final Instant effectiveAt) {
        final Instant now = Instant.now(clock);

        if (!effectiveAt.isAfter(now)) {
            throw new InvalidTermsOfServiceScheduleException(effectiveAt);
        }

        final TermsOfServiceEntity termsOfService = findByIdForUpdate(termsOfServiceId);
        ensureNoOtherScheduledTerms(termsOfServiceId);

        try {
            termsOfService.schedule(effectiveAt);
        } catch (final IllegalStateException exception) {
            throw new InvalidTermsOfServiceTransitionException(exception.getMessage());
        }

        return termsOfServiceRepository.save(termsOfService);
    }

    @Override
    @Transactional
    public TermsOfServiceEntity unschedule(final String termsOfServiceId) {
        final TermsOfServiceEntity termsOfService = findByIdForUpdate(termsOfServiceId);

        try {
            termsOfService.unschedule();
        } catch (final IllegalStateException exception) {
            throw new InvalidTermsOfServiceTransitionException(exception.getMessage());
        }

        return termsOfServiceRepository.save(termsOfService);
    }

    @Override
    @Transactional
    public TermsOfServiceEntity activateNow(final String termsOfServiceId) {
        final Instant now = Instant.now(clock);
        final TermsOfServiceEntity termsOfService = findByIdForUpdate(termsOfServiceId);
        final Optional<TermsOfServiceEntity> activeTermsOfService =
            termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.ACTIVE);

        activeTermsOfService.ifPresent(active -> supersedeIfDifferent(active, termsOfService, now));

        try {
            termsOfService.activate(now);
        } catch (final IllegalStateException exception) {
            throw new InvalidTermsOfServiceTransitionException(exception.getMessage());
        }

        return termsOfServiceRepository.save(termsOfService);
    }

    @Override
    @Transactional
    public TermsOfServiceEntity promoteDueScheduledTerms() {
        final Instant now = Instant.now(clock);
        final TermsOfServiceEntity dueScheduledTerms = termsOfServiceRepository
            .findDueScheduledTermsOfServiceForUpdate(now)
            .orElseThrow(NoDueScheduledTermsOfServiceException::new);
        final Optional<TermsOfServiceEntity> activeTermsOfService =
            termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.ACTIVE);

        activeTermsOfService.ifPresent(active -> supersedeIfDifferent(active, dueScheduledTerms, now));

        try {
            dueScheduledTerms.activate(dueScheduledTerms.getEffectiveAt());
        } catch (final IllegalStateException exception) {
            throw new InvalidTermsOfServiceTransitionException(exception.getMessage());
        }

        return termsOfServiceRepository.save(dueScheduledTerms);
    }

    private TermsOfServiceEntity findByIdForUpdate(final String termsOfServiceId) {
        return termsOfServiceRepository.findByIdForUpdate(termsOfServiceId)
            .orElseThrow(() -> new TermsOfServiceNotFoundException(termsOfServiceId));
    }

    private void ensureNoOtherScheduledTerms(final String termsOfServiceId) {
        termsOfServiceRepository.findByStatusForUpdate(TermsOfServiceStatus.SCHEDULED)
            .filter(scheduled -> !Objects.equals(scheduled.getId(), termsOfServiceId))
            .ifPresent(scheduled -> {
                throw new TermsOfServiceVersionAlreadyScheduledException(scheduled.getId());
            });
    }

    private void supersedeIfDifferent(
        final TermsOfServiceEntity activeTermsOfService,
        final TermsOfServiceEntity nextTermsOfService,
        final Instant now) {
        if (!Objects.equals(activeTermsOfService.getId(), nextTermsOfService.getId())) {
            activeTermsOfService.supersede(now);
            termsOfServiceRepository.save(activeTermsOfService);
        }
    }
}
