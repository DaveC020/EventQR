package com.thedavelopers.eventqr.features.organizer.events

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.thedavelopers.eventqr.features.organizer.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

open class EventManagementHubActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        val eventId = intentEventId() ?: return showMissingEventScreen("Event Management")
        val content = organizerShell("Event Management", showBack = true, darkHeader = true)
        content.addView(loadingState("Loading event details..."))

        MainScope().launch {
            val load = repository.loadEventForMvp(eventId)
            val event = load.data
            content.removeAllViews()
            dataSourceBanner(load)?.let { content.addView(it) }
            if (event == null) {
                content.addView(
                    if (load.source == OrganizerMvpDataSource.ERROR) {
                        errorState(load.message ?: "Event details could not be loaded.") { recreate() }
                    } else {
                        emptyState("Event not found or not available for organizer management.", "Open My Events") {
                            openOrganizerPage(ManageEventsActivity::class.java)
                        }
                    },
                )
                return@launch
            }

            // Custom header for Hub
            content.addView(LinearLayout(this@EventManagementHubActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, dp(16))
                addView(statusBadge(event.lifecycleStatus()))
                addView(text(event.title, 24, true, Color.WHITE).apply {
                    setPadding(0, dp(8), 0, dp(16))
                })
            })

            content.addView(row().apply {
                addView(summaryCard("Registered", formatCount(event.registeredCount)))
                addView(summaryCard("Capacity", formatCount(event.capacity), Color.parseColor("#94A3B8")))
                addView(summaryCard("Available", formatCount(event.availableSlots), SUCCESS))
            })

            content.addView(section("Event Management").apply { 
                setPadding(dp(2), dp(20), dp(2), dp(10))
            })
            
            val menuItems = listOf(
                Triple("Staff Assignment", com.thedavelopers.eventqr.R.drawable.ic_group, ManageUsersActivity::class.java),
                Triple("Scan Purposes", com.thedavelopers.eventqr.R.drawable.ic_qr_scan, ManageScanPurposesActivity::class.java),
                Triple("Transaction Rules", com.thedavelopers.eventqr.R.drawable.ic_fileedit, TransactionRulesActivity::class.java),
                Triple("ID Template", com.thedavelopers.eventqr.R.drawable.ic_file, IdTemplatePlaceholderActivity::class.java),
                Triple("Rewards", com.thedavelopers.eventqr.R.drawable.ic_gift, ManageRewardsActivity::class.java),
                Triple("Point Rules", com.thedavelopers.eventqr.R.drawable.ic_trend_up, PointRulesPlaceholderActivity::class.java),
                Triple("Attendees", com.thedavelopers.eventqr.R.drawable.ic_group, AttendeeManagementActivity::class.java),
                Triple("Reports", com.thedavelopers.eventqr.R.drawable.ic_trend_up, EventReportsActivity::class.java),
            )

            menuItems.forEach { (label, icon, target) ->
                content.addView(menuCard(label, icon) { openOrganizerPage(target, event.id, event.title) })
            }
        }
    }
}
