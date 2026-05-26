package com.thedavelopers.eventqr.features.admin

import android.content.Context
import com.thedavelopers.eventqr.core.api.ApiClient
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.api.safeApiCall
import com.thedavelopers.eventqr.features.events.model.dto.EventApprovalRequest
import com.thedavelopers.eventqr.features.events.model.dto.EventResponse
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpDataSource
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpEvent
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpLoad
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpPlaceholders
import com.thedavelopers.eventqr.core.util.DateFormatters
import com.thedavelopers.eventqr.core.api.dto.EventStatus

class AdminRepository(private val context: Context) {
    private val apiService = ApiClient.getService(context)

    suspend fun loadAllEventRequests(): OrganizerMvpLoad<List<OrganizerMvpEvent>> {
        return when (val result = safeApiCall { apiService.getEvents() }) {
            is NetworkResult.Success -> {
                val mapped = result.data.map { it.toMvpEvent() }
                OrganizerMvpLoad(mapped, OrganizerMvpDataSource.BACKEND)
            }
            is NetworkResult.Error -> mockLoad(OrganizerMvpPlaceholders.events, result.message)
            NetworkResult.Loading -> mockLoad(OrganizerMvpPlaceholders.events, null)
        }
    }

    suspend fun approveEvent(eventId: String, reviewerId: String? = null): NetworkResult<EventResponse> {
        val request = EventApprovalRequest(
            approved = true,
            reviewerUserId = reviewerId?.let { java.util.UUID.fromString(it) }
        )
        return safeApiCall { apiService.reviewEvent(eventId, request) }
    }

    suspend fun rejectEvent(eventId: String, reason: String, reviewerId: String? = null): NetworkResult<EventResponse> {
        val request = EventApprovalRequest(
            approved = false,
            rejectionReason = reason,
            reviewerUserId = reviewerId?.let { java.util.UUID.fromString(it) }
        )
        return safeApiCall { apiService.reviewEvent(eventId, request) }
    }

    private fun <T> mockLoad(data: T, message: String?): OrganizerMvpLoad<T> =
        OrganizerMvpLoad(data, OrganizerMvpDataSource.MOCK, message ?: OrganizerMvpPlaceholders.TODO_BACKEND)

    private fun EventResponse.toMvpEvent(): OrganizerMvpEvent = OrganizerMvpEvent(
        id = eventId.toString(),
        title = title,
        organizerName = "Organizer (ID: ${organizerUserId ?: "Unknown"})",
        dateTime = listOf(DateFormatters.formatInstant(eventStartAt), DateFormatters.formatInstant(eventEndAt))
            .filter { it != "-" }
            .joinToString(" - ")
            .ifBlank { "-" },
        shortDate = DateFormatters.formatInstant(eventStartAt),
        venue = location ?: "Venue not set",
        status = status.toDisplayStatus(),
        submittedDate = DateFormatters.formatInstant(registrationOpenAt),
        adminRemarks = rejectionReason ?: if (status == EventStatus.APPROVED) "Approved." else "No admin remarks.",
        additionalOrganizers = emptyList(),
        registeredCount = currentAttendeeCount,
        enteredCount = 0,
        attendedCount = 0,
        exitedCount = 0,
        noShowCount = 0,
        totalTransactions = 0,
        successfulScans = 0,
        rejectedScans = 0,
        benefitClaims = 0,
        boothSessionVisits = 0,
        rewardRedemptions = 0,
        totalPointsAwarded = 0,
        idTemplateStatus = "Backend managed",
        rewardsStatus = if (rewardsEnabled) "Enabled" else "Disabled",
        staffCount = 0,
        scanPurposesCount = 0,
    )

    private fun EventStatus.toDisplayStatus(): String = when (this) {
        EventStatus.APPROVED, EventStatus.ACTIVE -> "Approved"
        EventStatus.PENDING_REVIEW, EventStatus.DRAFT -> "Pending"
        EventStatus.REJECTED -> "Rejected"
        EventStatus.ENDED -> "Completed"
        EventStatus.CANCELLED -> "Rejected"
    }
}
