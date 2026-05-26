package com.thedavelopers.eventqr.features.dashboard.model.dto

import java.time.Instant
import java.util.UUID

data class DashboardSummary(
    val totalEvents: Long,
    val totalRegistrations: Long,
    val totalTransactions: Long,
    val totalRewards: Long,
    val totalNotifications: Long,
    val fullName: String? = null,
    val upcomingEvents: List<DashboardUpcomingEvent>? = emptyList(),
)

data class DashboardUpcomingEvent(
    val eventId: UUID,
    val registrationId: UUID? = null,
    val title: String,
    val location: String? = null,
    val eventStartAt: Instant? = null,
    val status: String? = null,
    val description: String? = null,
    val eventEndAt: Instant? = null,
    val capacity: Int = 0,
    val currentAttendeeCount: Int = 0,
)
