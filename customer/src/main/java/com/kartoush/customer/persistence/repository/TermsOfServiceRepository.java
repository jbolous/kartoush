package com.kartoush.customer.persistence.repository;

import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TermsOfServiceRepository extends JpaRepository<TermsOfServiceEntity, String> {

    Optional<TermsOfServiceEntity> findByVersion(String version);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select terms from TermsOfServiceEntity terms where terms.id = :id")
    Optional<TermsOfServiceEntity> findByIdForUpdate(@Param("id") String id);

    Optional<TermsOfServiceEntity> findByStatus(TermsOfServiceStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select terms from TermsOfServiceEntity terms where terms.status = :status")
    Optional<TermsOfServiceEntity> findByStatusForUpdate(@Param("status") TermsOfServiceStatus status);

    @Query(
        value = """
            select id,
                   version,
                   content,
                   content_type,
                   status,
                   effective_at,
                   superseded_at,
                   created_at,
                   updated_at
            from terms_of_service
            where status = 'SCHEDULED'
              and effective_at <= :effectiveAt
            for update
            """,
        nativeQuery = true
    )
    Optional<TermsOfServiceEntity> findDueScheduledTermsOfServiceForUpdate(@Param("effectiveAt") Instant effectiveAt);
}
