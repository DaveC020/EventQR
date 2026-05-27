package com.thedavelopers.eventqr.features.audit.model.dto;

import java.util.UUID;

public record AuditLogRequest(String action, String details, UUID eventId, UUID targetUserId) {
}
