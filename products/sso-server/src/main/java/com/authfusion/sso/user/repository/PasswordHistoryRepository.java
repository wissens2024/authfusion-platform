package com.authfusion.sso.user.repository;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.user.model.PasswordHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for PasswordHistoryEntity.
 */
@ToeScope(value = "패스워드 이력 저장소", sfr = {"FCS_COP.1"})
@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistoryEntity, UUID> {

    List<PasswordHistoryEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
