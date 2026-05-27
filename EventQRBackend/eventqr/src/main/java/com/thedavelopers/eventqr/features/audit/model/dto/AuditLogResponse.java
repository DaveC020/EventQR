package com.thedavelopers.eventqr.features.audit.model.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(UUID auditLogId, String action, String details, UUID performedByUserId, String performedByFullName, UUID eventId, UUID targetUserId, Instant timestamp) {
}
