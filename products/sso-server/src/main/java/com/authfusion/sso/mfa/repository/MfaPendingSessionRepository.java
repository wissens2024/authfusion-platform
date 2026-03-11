package com.authfusion.sso.mfa.repository;

import com.authfusion.sso.mfa.model.MfaPendingSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MfaPendingSessionRepository extends JpaRepository<MfaPendingSessionEntity, UUID> {

    Optional<MfaPendingSessionEntity> findByMfaToken(String mfaToken);

    void deleteByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM MfaPendingSessionEntity m WHERE m.expiresAt < :now")
    int deleteExpired(LocalDateTime now);
}
