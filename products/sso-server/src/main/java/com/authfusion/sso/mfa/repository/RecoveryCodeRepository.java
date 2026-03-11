package com.authfusion.sso.mfa.repository;

import com.authfusion.sso.mfa.model.RecoveryCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCodeEntity, UUID> {

    List<RecoveryCodeEntity> findByUserIdAndUsedFalse(UUID userId);

    int countByUserIdAndUsedFalse(UUID userId);

    void deleteByUserId(UUID userId);
}
