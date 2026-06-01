package com.thedavelopers.eventqr.features.organizer

import android.graphics.Color

internal fun OrganizerMvpAttendee.statusBucket(): String {
    val current = currentEventStatus.trim()
    val registration = registrationStatus.trim()
    return when {
        current.contains("checked in", ignoreCase = true) ||
            current.contains("entered", ignoreCase = true) ||
            current.contains("attended", ignoreCase = true) -> "Checked In"
        current.contains("exited", ignoreCase = true) || registration.equals("Exited", ignoreCase = true) -> "Exited"
        current.contains("no-show", ignoreCase = true) || current.contains("no show", ignoreCase = true) ||
            registration.contains("no-show", ignoreCase = true) || registration.contains("no show", ignoreCase = true) -> "No Show"
        registration.equals("Registered", ignoreCase = true) -> "Registered"
        current.isNotBlank() -> current
        registration.isNotBlank() -> registration
        else -> "Registered"
    }
}

internal fun OrganizerMvpAttendee.statusPalette(): Pair<Int, Int> = when (statusBucket()) {
    "Checked In" -> Color.parseColor("#D1FAE5") to Color.parseColor("#059669")
    "Exited" -> Color.parseColor("#E5E7EB") to Color.parseColor("#6B7280")
    "No Show" -> Color.parseColor("#FEF3C7") to Color.parseColor("#B45309")
    "Registered" -> Color.parseColor("#E0E7FF") to Color.parseColor("#4F46E5")
    else -> Color.parseColor("#E0E7FF") to Color.parseColor("#4F46E5")
}

internal fun OrganizerMvpAttendee.matchesOrganizerAttendeeQuery(query: String, filter: String): Boolean {
    val normalizedQuery = query.trim()
    val matchesFilter = when (filter) {
        "All" -> true
        "Registered" -> statusBucket().equals("Registered", ignoreCase = true)
        "Checked In" -> statusBucket().equals("Checked In", ignoreCase = true)
        "Exited" -> statusBucket().equals("Exited", ignoreCase = true)
        "No Show" -> statusBucket().equals("No Show", ignoreCase = true)
        else -> statusBucket().equals(filter, ignoreCase = true) || registrationStatus.equals(filter, ignoreCase = true)
    }
    if (!matchesFilter) return false
    if (normalizedQuery.isBlank()) return true
    return name.contains(normalizedQuery, ignoreCase = true) ||
        email.contains(normalizedQuery, ignoreCase = true) ||
        phone.contains(normalizedQuery, ignoreCase = true) ||
        id.contains(normalizedQuery, ignoreCase = true) ||
        registrationStatus.contains(normalizedQuery, ignoreCase = true) ||
        currentEventStatus.contains(normalizedQuery, ignoreCase = true) ||
        points.toString().contains(normalizedQuery, ignoreCase = true)
}

internal fun attendeeInitial(name: String): String = name.trim().firstOrNull()?.uppercase() ?: "?"

internal fun organizerEventDateLine(shortDate: String, eventTitle: String, venue: String): String {
    val rawDate = shortDate.trim()
    return rawDate
        .replace(Regex("T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z?"), "")
        .replace(Regex("\\s*·.*$"), "")
        .trim()
        .ifBlank { eventTitle.trim() }
}
