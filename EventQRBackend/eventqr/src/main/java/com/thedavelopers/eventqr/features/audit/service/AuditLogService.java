package com.thedavelopers.eventqr.features.audit.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.thedavelopers.eventqr.features.audit.model.dto.AuditLogRequest;
import com.thedavelopers.eventqr.features.audit.model.dto.AuditLogResponse;
import com.thedavelopers.eventqr.features.audit.model.entity.AuditLog;
import com.thedavelopers.eventqr.features.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public void log(AuditLogRequest request, UUID userId, String fullName) {
        AuditLog log = new AuditLog();
        log.setAction(request.action());
        log.setDetails(request.details());
        log.setPerformedByUserId(userId);
        log.setPerformedByFullName(fullName);
        log.setEventId(request.eventId());
        log.setTargetUserId(request.targetUserId());
        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> findAll() {
        return auditLogRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<AuditLogResponse> findByEvent(UUID eventId) {
        return auditLogRepository.findByEventId(eventId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getAction(), log.getDetails(), log.getPerformedByUserId(), log.getPerformedByFullName(), log.getEventId(), log.getTargetUserId(), log.getCreatedAt());
    }
}
