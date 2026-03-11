package com.authfusion.sso.session.store;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.session.model.SsoSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ToeScope(value = "세션 저장소 인터페이스", sfr = {"FIA_USB.1"})
public interface SessionStore {

    void save(SsoSession session);

    Optional<SsoSession> findById(String sessionId);

    List<SsoSession> findByUserId(UUID userId);

    void delete(String sessionId);

    void deleteByUserId(UUID userId);

    long countByUserId(UUID userId);
}
