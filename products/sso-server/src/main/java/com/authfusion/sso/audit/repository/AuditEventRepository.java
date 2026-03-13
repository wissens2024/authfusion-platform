package com.authfusion.sso.audit.repository;

import com.authfusion.sso.audit.model.AuditEventEntity;
import com.authfusion.sso.cc.ToeScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@ToeScope(value = "감사 이벤트 저장소", sfr = {"FAU_GEN.1", "FAU_SAR.1"})
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID>,
        JpaSpecificationExecutor<AuditEventEntity> {

    long countBySuccess(boolean success);

    @Query("SELECT a.eventType, COUNT(a) FROM AuditEventEntity a GROUP BY a.eventType")
    java.util.List<Object[]> countByEventType();

    @Query("SELECT a.action, COUNT(a) FROM AuditEventEntity a GROUP BY a.action")
    java.util.List<Object[]> countByAction();
}
