package com.authfusion.sso.jwt.repository;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.jwt.entity.SigningKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ToeScope(value = "JWT 서명키 저장소", sfr = {"FCS_CKM.1"})
@Repository
public interface SigningKeyRepository extends JpaRepository<SigningKeyEntity, UUID> {

    Optional<SigningKeyEntity> findByActiveTrue();

    Optional<SigningKeyEntity> findByKid(String kid);

    List<SigningKeyEntity> findAllByOrderByCreatedAtDesc();
}
