package com.authfusion.sso.audit.service;

import com.authfusion.sso.audit.model.AuditEventEntity;
import com.authfusion.sso.audit.model.AuditEventResponse;
import com.authfusion.sso.audit.model.AuditStatisticsResponse;
import com.authfusion.sso.audit.repository.AuditEventRepository;
import com.authfusion.sso.cc.ToeScope;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ToeScope(value = "감사 로그 서비스", sfr = {"FAU_GEN.1", "FAU_GEN.2"})
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    @Transactional
    public AuditEventEntity logEvent(AuditEventEntity event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }
        AuditEventEntity saved = auditEventRepository.save(event);
        log.debug("Audit event: {} - {} user={} success={}",
                event.getEventType(), event.getAction(), event.getUsername(), event.isSuccess());
        return saved;
    }

    public void logAuthentication(String action, String userId, String username,
                                   String ipAddress, boolean success, String errorMessage) {
        logEvent(AuditEventEntity.builder()
                .eventType("AUTHENTICATION")
                .action(action)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .success(success)
                .errorMessage(errorMessage)
                .build());
    }

    public void logUserManagement(String action, String actorId, String actorUsername,
                                   String resourceId, boolean success, String errorMessage) {
        logEvent(AuditEventEntity.builder()
                .eventType("USER_MANAGEMENT")
                .action(action)
                .userId(actorId)
                .username(actorUsername)
                .resourceType("USER")
                .resourceId(resourceId)
                .success(success)
                .errorMessage(errorMessage)
                .build());
    }

    public void logClientManagement(String action, String actorId, String actorUsername,
                                     String clientId, boolean success, String errorMessage) {
        logEvent(AuditEventEntity.builder()
                .eventType("CLIENT_MANAGEMENT")
                .action(action)
                .userId(actorId)
                .username(actorUsername)
                .resourceType("CLIENT")
                .resourceId(clientId)
                .success(success)
                .errorMessage(errorMessage)
                .build());
    }

    public void logTokenOperation(String action, String userId, String username,
                                   String clientId, boolean success, String errorMessage) {
        logEvent(AuditEventEntity.builder()
                .eventType("TOKEN_OPERATION")
                .action(action)
                .userId(userId)
                .username(username)
                .clientId(clientId)
                .success(success)
                .errorMessage(errorMessage)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> getEvents(String eventType, String userId, String username,
                                               String action, Boolean success,
                                               LocalDateTime from, LocalDateTime to,
                                               int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Specification<AuditEventEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (eventType != null && !eventType.isBlank())
                predicates.add(cb.equal(root.get("eventType"), eventType));
            if (userId != null && !userId.isBlank())
                predicates.add(cb.equal(root.get("userId"), userId));
            if (username != null && !username.isBlank())
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
            if (action != null && !action.isBlank())
                predicates.add(cb.equal(root.get("action"), action));
            if (success != null)
                predicates.add(cb.equal(root.get("success"), success));
            if (from != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            if (to != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return auditEventRepository.findAll(spec, pageable).map(AuditEventResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public AuditStatisticsResponse getStatistics() {
        long total = auditEventRepository.count();
        long successful = auditEventRepository.countBySuccess(true);
        long failed = auditEventRepository.countBySuccess(false);

        Map<String, Long> eventsByType = auditEventRepository.countByEventType().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

        Map<String, Long> eventsByAction = auditEventRepository.countByAction().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

        return AuditStatisticsResponse.builder()
                .totalEvents(total)
                .successfulEvents(successful)
                .failedEvents(failed)
                .eventsByType(eventsByType)
                .eventsByAction(eventsByAction)
                .build();
    }
}
