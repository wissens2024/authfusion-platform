package com.authfusion.sso.mfa.repository;

import com.authfusion.sso.mfa.model.TotpSecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TotpSecretRepository extends JpaRepository<TotpSecretEntity, UUID> {

    Optional<TotpSecretEntity> findByUserId(UUID userId);

    boolean existsByUserIdAndEnabledTrue(UUID userId);

    void deleteByUserId(UUID userId);
}
