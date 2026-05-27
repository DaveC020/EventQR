package com.thedavelopers.eventqr.features.audit.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.thedavelopers.eventqr.features.audit.model.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByEventId(UUID eventId);
}
