package com.kartoush.jobs.persistence.repository;

import com.kartoush.jobs.persistence.entity.BackgroundJobOutboxEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface BackgroundJobOutboxRepository extends JpaRepository<BackgroundJobOutboxEntity, UUID> {
    @Query(value = "select * from background_job_outbox where dispatched_at is null and completed_at is null "
        + "and quarantined_at is null and available_at <= current_timestamp "
        + "and (next_publication_at is null or next_publication_at <= current_timestamp) "
        + "order by available_at, created_at "
        + "limit 1 for update skip locked", nativeQuery = true)
    Optional<BackgroundJobOutboxEntity> lockNextPending();

    long countByQuarantinedAtIsNotNullAndCompletedAtIsNull();

    @Query("select min(j.quarantinedAt) from BackgroundJobOutboxEntity j "
        + "where j.quarantinedAt is not null and j.completedAt is null")
    Optional<Instant> oldestQuarantinedAt();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from BackgroundJobOutboxEntity j where j.id = :id")
    Optional<BackgroundJobOutboxEntity> lockById(@Param("id") UUID id);
}
