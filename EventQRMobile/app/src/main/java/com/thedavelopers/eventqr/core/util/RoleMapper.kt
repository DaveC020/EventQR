package com.thedavelopers.eventqr.core.util

import com.thedavelopers.eventqr.core.api.dto.AccountRole

object RoleMapper {
    /**
     * Normalizes a role string from the backend into a consistent uppercase format.
     * Maps 'USER' variations to 'ATTENDEE'.
     */
    fun normalizeRole(role: String?): String {
        if (role.isNullOrBlank()) return ""
        
        val upper = role.trim().uppercase()
        return when (upper) {
            "USER", "ATTENDEE" -> AccountRole.ATTENDEE.name
            "STAFF" -> AccountRole.STAFF.name
            "ORGANIZER" -> AccountRole.ORGANIZER.name
            "ADMIN" -> AccountRole.ADMIN.name
            "SUPER_ADMIN", "SUPERADMIN" -> AccountRole.SUPER_ADMIN.name
            else -> upper // Return as-is if unknown, let the router handle it
        }
    }

    /**
     * Maps a normalized role string to a displayable name.
     */
    fun getDisplayName(role: String?): String {
        val normalized = normalizeRole(role)
        return when (normalized) {
            AccountRole.ATTENDEE.name -> "Attendee"
            AccountRole.STAFF.name -> "Staff"
            AccountRole.ORGANIZER.name -> "Organizer"
            AccountRole.ADMIN.name -> "Administrator"
            AccountRole.SUPER_ADMIN.name -> "Super Admin"
            else -> normalized.lowercase().capitalize()
        }
    }
}
