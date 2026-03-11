package com.authfusion.sso.user.repository;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.user.model.UserEntity;
import com.authfusion.sso.user.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for UserEntity.
 */
@ToeScope(value = "사용자 저장소", sfr = {"FIA_UAU.1"})
@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<UserEntity> findByStatus(UserStatus status);
}
