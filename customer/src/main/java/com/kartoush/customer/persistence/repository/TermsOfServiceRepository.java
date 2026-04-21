package com.kartoush.customer.persistence.repository;

import com.kartoush.customer.persistence.entity.TermsOfServiceEntity;
import com.kartoush.customer.termsofservice.TermsOfServiceStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TermsOfServiceRepository extends JpaRepository<TermsOfServiceEntity, String> {

    Optional<TermsOfServiceEntity> findByVersion(String version);

    Optional<TermsOfServiceEntity> findByStatus(TermsOfServiceStatus status);

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
            """,
        nativeQuery = true
    )
    Optional<TermsOfServiceEntity> findDueScheduledTermsOfService(@Param("effectiveAt") Instant effectiveAt);
}
