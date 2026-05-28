package com.thedavelopers.eventqr.features.transactions.model.dto

import com.thedavelopers.eventqr.core.api.dto.TransactionResult
import com.thedavelopers.eventqr.core.api.dto.TransactionType
import java.time.Instant
import java.util.UUID

data class TransactionRequest(
    val eventId: UUID,
    val scanPurposeId: UUID,
    val qrValue: String,
    val staffUserId: UUID? = null,
    val notes: String? = null,
)

data class TransactionResponse(
    val transactionId: UUID,
    val eventId: UUID,
    val eventTitle: String? = null,
    val attendeeUserId: UUID,
    val attendeeName: String? = null,
    val registrationId: UUID,
    val registrationStatus: String? = null,
    val qrCredentialId: UUID,
    val scanPurposeId: UUID,
    val scanPurposeName: String? = null,
    val transactionType: TransactionType,
    val transactionResult: TransactionResult,
    val pointsDelta: Int,
    val reason: String? = null,
    val scannedAt: Instant? = null,
)
