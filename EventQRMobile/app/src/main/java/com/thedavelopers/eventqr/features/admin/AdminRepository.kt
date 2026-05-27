package com.thedavelopers.eventqr.features.admin

import android.content.Context
import com.thedavelopers.eventqr.core.api.ApiClient
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.api.safeApiCall
import com.thedavelopers.eventqr.core.api.dto.EventRequestStatus
import com.thedavelopers.eventqr.features.events.model.dto.EventRequestDecisionRequest
import com.thedavelopers.eventqr.features.events.model.dto.EventRequestResponse
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpDataSource
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpEvent
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpLoad
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpPlaceholders
import com.thedavelopers.eventqr.core.util.DateFormatters

class AdminRepository(private val context: Context) {
    private val apiService = ApiClient.getService(context)

    suspend fun loadAllEventRequests(): OrganizerMvpLoad<List<OrganizerMvpEvent>> {
        return when (val result = safeApiCall { apiService.getAdminEventRequests() }) {
            is NetworkResult.Success -> {
                val mapped = result.data.map { it.toMvpEvent() }
                OrganizerMvpLoad(mapped, OrganizerMvpDataSource.BACKEND)
            }
            is NetworkResult.Error -> mockLoad(OrganizerMvpPlaceholders.events, result.message)
            NetworkResult.Loading -> mockLoad(OrganizerMvpPlaceholders.events, null)
        }
    }

    suspend fun approveEvent(eventId: String, reviewerId: String? = null): NetworkResult<EventRequestResponse> {
        return safeApiCall { apiService.approveEventRequest(eventId, EventRequestDecisionRequest("Approved.")) }
    }

    suspend fun rejectEvent(eventId: String, reason: String, reviewerId: String? = null): NetworkResult<EventRequestResponse> {
        return safeApiCall { apiService.rejectEventRequest(eventId, EventRequestDecisionRequest(reason)) }
    }

    private fun <T> mockLoad(data: T, message: String?): OrganizerMvpLoad<T> =
        OrganizerMvpLoad(data, OrganizerMvpDataSource.MOCK, message ?: OrganizerMvpPlaceholders.TODO_BACKEND)

    private fun EventRequestResponse.toMvpEvent(): OrganizerMvpEvent = OrganizerMvpEvent(
        id = eventRequestId.toString(),
        title = eventName,
        organizerName = requesterName ?: "Requester (ID: $requesterUserId)",
        dateTime = listOf(DateFormatters.formatInstant(startDateTime), DateFormatters.formatInstant(endDateTime))
            .filter { it != "-" }
            .joinToString(" - ")
            .ifBlank { "-" },
        shortDate = DateFormatters.formatInstant(startDateTime),
        venue = venue ?: "Venue not set",
        status = status.toDisplayStatus(),
        submittedDate = DateFormatters.formatInstant(createdAt),
        adminRemarks = adminRemarks ?: if (status == EventRequestStatus.APPROVED) "Approved." else "No admin remarks.",
        additionalOrganizers = emptyList(),
        registeredCount = capacity,
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
        idTemplateStatus = if (requestedFeatures.orEmpty().contains("ID printing")) "Requested" else "Not requested",
        rewardsStatus = if (requestedFeatures.orEmpty().contains("Rewards and points")) "Requested" else "Not requested",
        staffCount = 0,
        scanPurposesCount = 0,
    )

    private fun EventRequestStatus.toDisplayStatus(): String = when (this) {
        EventRequestStatus.APPROVED -> "Approved"
        EventRequestStatus.PENDING -> "Pending"
        EventRequestStatus.REJECTED -> "Rejected"
    }
}
