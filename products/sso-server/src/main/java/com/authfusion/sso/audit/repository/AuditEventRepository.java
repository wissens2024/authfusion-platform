package com.authfusion.sso.audit.repository;

import com.authfusion.sso.audit.model.AuditEventEntity;
import com.authfusion.sso.cc.ToeScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@ToeScope(value = "감사 이벤트 저장소", sfr = {"FAU_GEN.1", "FAU_SAR.1"})
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    @Query("SELECT a FROM AuditEventEntity a WHERE " +
            "(:eventType IS NULL OR a.eventType = :eventType) AND " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:success IS NULL OR a.success = :success) AND " +
            "(:from IS NULL OR a.timestamp >= :from) AND " +
            "(:to IS NULL OR a.timestamp <= :to)")
    Page<AuditEventEntity> findByFilters(
            @Param("eventType") String eventType,
            @Param("userId") String userId,
            @Param("action") String action,
            @Param("success") Boolean success,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    long countBySuccess(boolean success);

    @Query("SELECT a.eventType, COUNT(a) FROM AuditEventEntity a GROUP BY a.eventType")
    java.util.List<Object[]> countByEventType();

    @Query("SELECT a.action, COUNT(a) FROM AuditEventEntity a GROUP BY a.action")
    java.util.List<Object[]> countByAction();
}
