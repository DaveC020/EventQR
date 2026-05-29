package com.thedavelopers.eventqr.features.organizer.reports

import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.features.organizer.*
import com.thedavelopers.eventqr.features.organizer.model.dto.OrganizerReportDto
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

open class EventReportsActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var selectedEvent: OrganizerMvpEvent
    private lateinit var report: LinearLayout
    private var reportSource: OrganizerMvpLoad<OrganizerMvpEvent>? = null
    private var reportDto: OrganizerReportDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        val eventId = intentEventId() ?: return showMissingEventScreen("Event Reports")
        selectedEvent = resolveSelectedEvent(repository.getApprovedOrganizerEvents(), eventId) ?: return showMissingEventScreen("Event Reports")
        val content = organizerShell("Event Reports", selectedEvent.title, NAV_REPORTS, topRightLabel = "Export") {
            // TODO: Connect to backend export/download implementation.
            Toast.makeText(this, "Export/download placeholder", Toast.LENGTH_SHORT).show()
        }
        report = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(card().apply {
            addView(text("Select Event", 13, false, MUTED))
            addView(eventSelector(repository.getApprovedOrganizerEvents(), selectedEvent.id) {
                selectedEvent = it
                repository.saveSelectedEventId(it.id)
                saveSelectedEventId(it.id)
                loadReport()
            })
        })
        content.addView(report)
        content.addView(primaryButton("Generate Report") {
            loadReport()
        })
        content.addView(primaryButton("Export / Download Report") {
            // TODO: Connect to backend export/download implementation.
            Toast.makeText(this, "Export/download placeholder", Toast.LENGTH_SHORT).show()
        })
        report.addView(loadingState("Loading report..."))
        loadReport()
    }

    private fun render() {
        val reportEvent = reportSource?.data ?: selectedEvent
        val liveReport = reportDto
        report.removeAllViews()
        reportSource?.let { dataSourceBanner(it)?.let { banner -> report.addView(banner) } }
        if (liveReport == null && reportSource?.source != OrganizerMvpDataSource.BACKEND) {
            report.addView(emptyState("Reports will appear after registrations and scans are recorded."))
            return
        }
        val isEmptyReport = liveReport != null &&
            liveReport.totalRegistered == 0 &&
            liveReport.enteredCount == 0 &&
            liveReport.exitedCount == 0 &&
            liveReport.approvedTransactionCount == 0 &&
            liveReport.rejectedTransactionCount == 0
        if (isEmptyReport) {
            report.addView(emptyState("Reports will appear after registrations and scans are recorded."))
            return
        }
        report.addView(row().apply {
            addView(summaryCard("Registered", (liveReport?.totalRegistered ?: reportEvent.registeredCount).toString(), Color.parseColor("#3B82F6")))
            addView(summaryCard("Entered", (liveReport?.enteredCount ?: reportEvent.enteredCount).toString(), SUCCESS))
        })
        report.addView(row().apply {
            addView(summaryCard("No Shows", (liveReport?.noShowCount ?: reportEvent.noShowCount).toString(), ERROR))
            addView(summaryCard("Points Earned", (liveReport?.pointsDistributed ?: reportEvent.totalPointsAwarded).toString(), Color.parseColor("#F59E0B")))
        })
        report.addView(row().apply {
            addView(summaryCard("Approved Txns", (liveReport?.approvedTransactionCount ?: reportEvent.successfulScans).toString(), PURPLE))
            addView(summaryCard("Rejected Txns", (liveReport?.rejectedTransactionCount ?: reportEvent.rejectedScans).toString(), ERROR))
        })
        report.addView(row().apply {
            addView(summaryCard("Booth Visits", (liveReport?.boothSessionVisits ?: reportEvent.boothSessionVisits).toString(), PRIMARY))
            addView(summaryCard("Rewards", (liveReport?.rewardRedemptions ?: reportEvent.rewardRedemptions).toString(), SUCCESS))
        })
        report.addView(reportSection("QR Transaction Summary", liveReport?.transactionSummary?.map { it.label to it.value } ?: listOf(
            "Approved Scans" to reportEvent.successfulScans.toString(),
            "Rejected Scans" to reportEvent.rejectedScans.toString(),
        )))
        report.addView(reportSection("Attendance Summary", liveReport?.attendanceSummary?.map { it.label to it.value } ?: listOf(
            "Registered" to reportEvent.registeredCount.toString(),
            "Entered" to reportEvent.enteredCount.toString(),
            "Exited" to reportEvent.exitedCount.toString(),
            "Attendance Count" to reportEvent.attendedCount.toString(),
            "No Shows" to reportEvent.noShowCount.toString(),
        )))
        report.addView(reportSection("Rejected Transaction Summary", liveReport?.rejectedSummary?.map { it.label to it.value } ?: listOf(
            "Rejected scans" to reportEvent.rejectedScans.toString(),
        )))
        report.addView(reportSection("Points and Rewards Summary", liveReport?.pointsRewardsSummary?.map { it.label to it.value } ?: listOf(
            "Points earned" to reportEvent.totalPointsAwarded.toString(),
            "Reward redemptions" to reportEvent.rewardRedemptions.toString(),
        )))
        val recent = liveReport?.recentActivity?.map { it.label to it.value }.orEmpty()
        report.addView(reportSection("Recent Activity", recent.ifEmpty { listOf("No recent activity" to "-") }))
        report.addView(stateCard())
    }

    private fun loadReport() {
        report.removeAllViews()
        report.addView(loadingState("Loading report..."))
        MainScope().launch {
            val live = repository.fetchOrganizerReport(selectedEvent.id)
            reportDto = (live as? NetworkResult.Success)?.data
            reportSource = repository.loadReportForMvp(selectedEvent)
            render()
        }
    }

    private fun reportSection(title: String, rows: List<Pair<String, String>>): LinearLayout =
        card().apply {
            addView(text(title, 18, true))
            rows.forEach { (label, value) ->
                addView(row().apply {
                    setPadding(0, dp(6), 0, dp(6))
                    addView(text(label, 14, false, MUTED).apply {
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    addView(text(value, 15, true))
                })
            }
        }
}
