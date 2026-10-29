package com.kartoush.jobs.service;

import com.kartoush.jobs.persistence.repository.BackgroundJobOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class BackgroundJobOutboxRecoveryService {
    private final BackgroundJobOutboxRepository repository;

    public BackgroundJobOutboxRecoveryService(final BackgroundJobOutboxRepository repository) {
        this.repository = repository;
    }

    /** Retry the same intent after an operator resolves the publication failure. */
    @Transactional
    public void retryPublication(final UUID id) {
        repository.lockById(id).orElseThrow().retryPublication();
    }
}
