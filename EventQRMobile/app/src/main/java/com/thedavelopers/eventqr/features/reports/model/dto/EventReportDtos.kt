package com.thedavelopers.eventqr.features.reports.model.dto

data class EventReportSnapshot(
    val totalAttendees: Int = 0,
    val registeredCount: Int = 0,
    val enteredCount: Int = 0,
    val exitedCount: Int = 0,
    val noShowCount: Int = 0,
    val attendanceCount: Int = 0,
    val claimsCount: Int = 0,
    val boothSessionVisits: Int = 0,
    val rewardsRedeemed: Int = 0,
    val totalPointsEarned: Int = 0,
    val approvedTransactions: Int = 0,
    val rejectedTransactions: Int = 0,
)
