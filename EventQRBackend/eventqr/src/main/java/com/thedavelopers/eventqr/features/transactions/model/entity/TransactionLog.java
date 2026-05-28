package com.thedavelopers.eventqr.features.transactions.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.thedavelopers.eventqr.shared.constants.TransactionResult;
import com.thedavelopers.eventqr.shared.constants.TransactionType;
import com.thedavelopers.eventqr.shared.utils.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "transaction_logs")
public class TransactionLog extends BaseEntity {

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private UUID attendeeUserId;

    @Column(nullable = false)
    private UUID registrationId;

    @Column(nullable = false)
    private UUID qrCredentialId;

    @Column(nullable = false)
    private UUID scanPurposeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionResult transactionResult;

    private UUID staffUserId;

    private Instant scannedAt;

    @Column(length = 2000)
    private String reason;

    @Column(nullable = false, length = 4000)
    private String metadata = "{}";

    @Column(nullable = false)
    private int pointsDelta;

    @PrePersist
    @PreUpdate
    void ensureDefaults() {
        if (metadata == null || metadata.isBlank()) {
            metadata = "{}";
        }
        if (scannedAt == null) {
            scannedAt = Instant.now();
        }
    }
}
