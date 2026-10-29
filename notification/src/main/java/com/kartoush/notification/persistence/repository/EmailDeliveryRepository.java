package com.kartoush.notification.persistence.repository;

import com.kartoush.notification.persistence.entity.EmailDeliveryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailDeliveryRepository extends JpaRepository<EmailDeliveryEntity, UUID> {
    @Modifying
    @Query(value = "insert into email_delivery (id, provider, status, attempt_count, attempt_started_at, updated_at) "
        + "values (:id, :provider, 'RETRYABLE', 0, :now, :now) on conflict (id) do nothing", nativeQuery = true)
    void insertIfAbsent(@Param("id") UUID id, @Param("provider") String provider, @Param("now") Instant now);

    @Modifying
    @Query(value = "insert into email_delivery_attempt (delivery_id, attempt_number, status, started_at) "
        + "values (:id, :number, 'SENDING', :now)", nativeQuery = true)
    void recordAttempt(@Param("id") UUID id, @Param("number") int number, @Param("now") Instant now);

    @Modifying
    @Query(value = "update email_delivery_attempt set status = :outcome, finished_at = :now, "
        + "provider_message_id = :receipt where delivery_id = :id and attempt_number = :number", nativeQuery = true)
    void recordOutcome(@Param("id") UUID id, @Param("number") int number, @Param("outcome") String outcome,
                       @Param("receipt") String receipt, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from EmailDeliveryEntity d where d.id = :id")
    Optional<EmailDeliveryEntity> lockById(@Param("id") UUID id);
}
