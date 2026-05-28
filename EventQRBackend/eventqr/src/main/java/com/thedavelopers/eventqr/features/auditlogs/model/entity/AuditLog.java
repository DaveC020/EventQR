package com.thedavelopers.eventqr.features.auditlogs.model.entity;

import java.util.UUID;
import com.thedavelopers.eventqr.shared.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private UUID performedByUserId;

    private String performedByFullName;

    private UUID eventId;

    private UUID targetUserId;
}

