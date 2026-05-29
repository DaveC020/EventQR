package com.thedavelopers.eventqr.features.dashboard

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.dto.AccountRole
import com.thedavelopers.eventqr.core.session.SessionManager
import com.thedavelopers.eventqr.core.util.DateFormatters
import com.thedavelopers.eventqr.core.util.RoleMapper
import com.thedavelopers.eventqr.features.attendee.AttendeeBottomNavItem
import com.thedavelopers.eventqr.features.attendee.AttendeeRepository
import com.thedavelopers.eventqr.features.attendee.EXTRA_EVENT_CAPACITY
import com.thedavelopers.eventqr.features.attendee.EXTRA_EVENT_CATEGORY
import com.thedavelopers.eventqr.features.attendee.EXTRA_EVENT_COUNT
import com.thedavelopers.eventqr.features.attendee.EXTRA_EVENT_DESCRIPTION
import com.thedavelopers.eventqr.features.attendee.EXTRA_EVENT_END
import com.thedavelopers.eventqr.features.attendee.EXTRA_EVENT_ID
import com.thedavelopers.eventqr.features.attendee.EXTRA_EVENT_LOCATION
import com.thedavelopers.eventqr.features.attendee.EXTRA_EVENT_START
import com.thedavelopers.eventqr.features.attendee.EXTRA_EVENT_STATUS
import com.thedavelopers.eventqr.features.attendee.EXTRA_EVENT_TITLE
import com.thedavelopers.eventqr.features.attendee.configureAttendeeBottomNav
import com.thedavelopers.eventqr.features.dashboard.model.dto.DashboardSummary
import com.thedavelopers.eventqr.features.dashboard.model.dto.DashboardUpcomingEvent
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

open class DashboardActivity : AppCompatActivity(), DashboardContract.View {
    private lateinit var presenter: DashboardPresenter
    private lateinit var sessionManager: SessionManager
    private lateinit var welcomeText: TextView
    private lateinit var nameText: TextView
    private lateinit var summaryEvents: TextView
    private lateinit var summaryRegistrations: TextView
    private lateinit var summaryRewards: TextView
    private lateinit var loadingText: TextView
    private lateinit var attendeeCard: View
    private lateinit var staffCard: View
    private lateinit var organizerCard: View
    private lateinit var notificationsCard: View
    private lateinit var notificationBell: ImageView
    private lateinit var notificationDot: View
    private lateinit var upcomingEventsLayout: LinearLayout
    private lateinit var discoverEventsLayout: LinearLayout
    private lateinit var upcomingEventsViewAll: TextView
    private lateinit var discoverEventsSeeAll: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var isSwipeRefreshing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)
        configureAttendeeBottomNav(AttendeeBottomNavItem.DASHBOARD)

        sessionManager = SessionManager(this)
        presenter = DashboardPresenter(
            this,
            DashboardRepository(this),
            AttendeeRepository(this),
            sessionManager,
        )
        presenter.attach(this)

        welcomeText = findViewById(R.id.txtDashboardWelcome)
        nameText = findViewById(R.id.txtDashboardName)
        summaryEvents = findViewById(R.id.txtTotalEvents)
        summaryRegistrations = findViewById(R.id.txtTotalRegistrations)
        summaryRewards = findViewById(R.id.txtTotalRewards)
        loadingText = findViewById(R.id.txtDashboardLoading)
        attendeeCard = findViewById(R.id.btnAttendeeHub)
        staffCard = findViewById(R.id.btnStaffHub)
        organizerCard = findViewById(R.id.btnTransactionHistory)
        notificationsCard = findViewById(R.id.btnNotificationsHub)
        notificationBell = findViewById(R.id.btnDashboardNotifications)
        notificationDot = findViewById(R.id.viewNotificationDot)
        upcomingEventsLayout = findViewById(R.id.layoutUpcomingEvents)
        discoverEventsLayout = findViewById(R.id.layoutDiscoverEvents)
        discoverEventsSeeAll = findViewById(R.id.txtDiscoverEventsSeeAll)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshDashboard)

        swipeRefreshLayout.setColorSchemeResources(R.color.eventqr_purple)
        swipeRefreshLayout.setOnRefreshListener {
            isSwipeRefreshing = true
            presenter.loadDashboard()
        }

        notificationBell.setOnClickListener {
            startActivity(Intent(this, com.thedavelopers.eventqr.features.attendee.AttendeeNotificationsActivity::class.java))
        }
        discoverEventsSeeAll.setOnClickListener {
            startActivity(Intent(this, com.thedavelopers.eventqr.features.attendee.AttendeeEventsActivity::class.java))
        }

        setupPortalSwitcher()
        configureActions()
        presenter.loadDashboard()
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    override fun showLoading(isLoading: Boolean) {
        if (isSwipeRefreshing) {
            if (!isLoading) {
                stopSwipeRefresh()
            }
            loadingText.visibility = View.GONE
            return
        }

        loadingText.text = "Loading dashboard..."
        loadingText.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun showSummary(summary: DashboardSummary) {
        stopSwipeRefresh()
        loadingText.visibility = View.GONE

        nameText.text = summary.fullName?.takeIf { it.isNotBlank() }
            ?: sessionManager.getFullName()?.takeIf { it.isNotBlank() }
            ?: "Attendee"

        summaryEvents.text = summary.totalEvents.toString()
        summaryRegistrations.text = summary.totalRegistrations.toString()
        summaryRewards.text = summary.totalRewards.toString()
        notificationDot.visibility = if (summary.totalNotifications > 0) View.VISIBLE else View.GONE

        renderUpcomingEvents(summary.upcomingEvents.orEmpty())
        renderDiscoverEvents(summary.discoverEvents.orEmpty())
    }

    override fun showError(message: String) {
        stopSwipeRefresh()
        loadingText.text = message
        loadingText.visibility = View.VISIBLE
        renderUpcomingEvents(emptyList())
        renderDiscoverEvents(emptyList())
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun openSection(title: String, message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun updateHeader(role: String?, name: String?) {
        welcomeText.text = "Welcome back,"
        nameText.text = name?.takeIf { it.isNotBlank() } ?: "Attendee"
    }

    private fun renderUpcomingEvents(events: List<DashboardUpcomingEvent>) {
        while (upcomingEventsLayout.childCount > 1) {
            upcomingEventsLayout.removeViewAt(1)
        }

        if (events.isEmpty()) {
            upcomingEventsLayout.addView(createEmptyStateView("No upcoming events yet."))
            return
        }

        events.forEachIndexed { index, event ->
            upcomingEventsLayout.addView(createUpcomingEventRow(event, index == 0))
        }
    }

    private fun renderDiscoverEvents(events: List<DashboardUpcomingEvent>) {
        while (discoverEventsLayout.childCount > 1) {
            discoverEventsLayout.removeViewAt(1)
        }

        if (events.isEmpty()) {
            discoverEventsLayout.addView(createEmptyStateView("No discoverable events right now."))
            return
        }

        events.forEachIndexed { index, event ->
            discoverEventsLayout.addView(createDiscoverEventCard(event, index == 0))
        }
    }

    private fun createUpcomingEventRow(event: DashboardUpcomingEvent, isFirst: Boolean): View {
        val row = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = if (isFirst) dp(14) else dp(10)
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            isClickable = true
            isFocusable = true
            setOnClickListener { openEventDetail(event) }
        }

        row.addView(FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            setBackgroundResource(R.drawable.bg_event_date_upcoming)
            addView(ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
                setImageResource(R.drawable.ic_qr_scan)
                setColorFilter(0xFF4F46E5.toInt())
            })
        })

        val textColumn = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
                marginEnd = dp(8)
            }
            orientation = LinearLayout.VERTICAL
        }

        textColumn.addView(TextView(this).apply {
            text = event.title.ifBlank { "Untitled event" }
            setTextColor(0xFF111827.toInt())
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
        })

        textColumn.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(4)
            }
            text = DateFormatters.formatInstant(event.eventStartAt)
            setTextColor(0xFF6B7280.toInt())
            textSize = 13f
        })
        row.addView(textColumn)

        val badgeLabel = if (event.isRegistered) "Registered" else (event.status ?: "Upcoming")
        val badge = TextView(this).apply {
            text = badgeLabel
            setPadding(dp(12), dp(5), dp(12), dp(5))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        applyStatusBadgeStyle(badge, badgeLabel, event.isRegistered)
        row.addView(badge)
        return row
    }

    private fun createDiscoverEventCard(event: DashboardUpcomingEvent, isFirst: Boolean): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_attendee_event, discoverEventsLayout, false)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = if (isFirst) dp(12) else 0
            bottomMargin = dp(12)
        }
        view.layoutParams = params

        val titleView = view.findViewById<TextView>(R.id.txtAttendeeEventTitle)
        val statusView = view.findViewById<TextView>(R.id.txtAttendeeEventStatus)
        val dateTimeView = view.findViewById<TextView>(R.id.txtAttendeeEventDateTime)
        val locationView = view.findViewById<TextView>(R.id.txtAttendeeEventLocation)
        val topStrip = view.findViewById<View>(R.id.viewEventTopStrip)
        val dateLayout = view.findViewById<View>(R.id.layoutEventDate)
        val dayView = view.findViewById<TextView>(R.id.txtEventDay)
        val monthView = view.findViewById<TextView>(R.id.txtEventMonth)
        val regCountView = view.findViewById<TextView>(R.id.txtRegistrationCount)
        val regPercentView = view.findViewById<TextView>(R.id.txtRegistrationPercent)
        val progressBar = view.findViewById<ProgressBar>(R.id.pbRegistration)

        val statusLabel = event.status ?: "Upcoming"
        titleView.text = event.title.ifBlank { "Untitled event" }
        statusView.text = statusLabel
        applyEventStatusUi(statusLabel, statusView, topStrip, dateLayout, dayView, monthView, progressBar)

        val manila = ZoneId.of("Asia/Manila")
        val startAt = event.eventStartAt
        if (startAt != null) {
            val zdt = startAt.atZone(manila)
            dayView.text = zdt.dayOfMonth.toString()
            monthView.text = zdt.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH)
            dateTimeView.text = zdt.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
        } else {
            dayView.text = "--"
            monthView.text = "---"
            dateTimeView.text = "-"
        }

        locationView.text = event.location?.takeIf { it.isNotBlank() } ?: "Location not set"

        if (event.capacity > 0) {
            val percentage = ((event.currentAttendeeCount.toFloat() / event.capacity.toFloat()) * 100f)
                .toInt()
                .coerceIn(0, 100)
            regCountView.text = "${event.currentAttendeeCount} / ${event.capacity} registered"
            regPercentView.text = "$percentage%"
            regPercentView.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            progressBar.progress = percentage
        } else {
            regCountView.text = "${event.currentAttendeeCount} registered"
            regPercentView.visibility = View.GONE
            progressBar.visibility = View.GONE
        }

        view.setOnClickListener { openEventDetail(event) }
        return view
    }

    private fun createEmptyStateView(message: String): View {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(14)
            }
            gravity = Gravity.CENTER
            text = message
            setTextColor(0xFF6B7280.toInt())
            textSize = 14f
            setPadding(dp(12), dp(20), dp(12), dp(20))
        }
    }

    private fun applyStatusBadgeStyle(textView: TextView, label: String, isRegistered: Boolean) {
        when {
            isRegistered -> {
                textView.setBackgroundResource(R.drawable.bg_event_status_registered)
                textView.setTextColor(0xFF4F46E5.toInt())
            }
            label.equals("Completed", true) -> {
                textView.setBackgroundResource(R.drawable.bg_event_status_completed)
                textView.setTextColor(0xFF10B981.toInt())
            }
            label.equals("Active", true) -> {
                textView.setBackgroundResource(R.drawable.bg_event_status_active)
                textView.setTextColor(0xFF06B6D4.toInt())
            }
            else -> {
                textView.setBackgroundResource(R.drawable.bg_event_status_upcoming)
                textView.setTextColor(0xFF4F46E5.toInt())
            }
        }
    }

    private fun applyEventStatusUi(
        status: String,
        statusView: TextView,
        topStrip: View,
        dateLayout: View,
        dayView: TextView,
        monthView: TextView,
        progressBar: ProgressBar,
    ) {
        val isCompleted = status.equals("Completed", true)
        val isActive = status.equals("Active", true) || status.equals("Ongoing", true)

        val primaryColor = when {
            isCompleted -> 0xFF10B981.toInt()
            isActive -> 0xFF06B6D4.toInt()
            else -> 0xFF4F46E5.toInt()
        }

        statusView.setBackgroundResource(
            when {
                isCompleted -> R.drawable.bg_event_status_completed
                isActive -> R.drawable.bg_event_status_active
                else -> R.drawable.bg_event_status_upcoming
            },
        )
        statusView.setTextColor(primaryColor)

        topStrip.setBackgroundColor(primaryColor)
        dayView.setTextColor(primaryColor)
        monthView.setTextColor(primaryColor)

        dateLayout.setBackgroundResource(
            when {
                isCompleted -> R.drawable.bg_event_date_completed
                isActive -> R.drawable.bg_event_date_active
                else -> R.drawable.bg_event_date_upcoming
            },
        )

        progressBar.progressDrawable = getDrawable(
            when {
                isCompleted -> R.drawable.pb_event_completed
                isActive -> R.drawable.pb_event_active
                else -> R.drawable.pb_event_upcoming
            },
        )
    }

    private fun openEventDetail(event: DashboardUpcomingEvent) {
        val intent = Intent(this, com.thedavelopers.eventqr.features.attendee.EventDetailActivity::class.java).apply {
            putExtra(EXTRA_EVENT_ID, event.eventId.toString())
            putExtra(EXTRA_EVENT_TITLE, event.title)
            putExtra(EXTRA_EVENT_LOCATION, event.location ?: "")
            putExtra(EXTRA_EVENT_DESCRIPTION, event.description ?: "")
            putExtra(EXTRA_EVENT_CATEGORY, event.category ?: "")
            putExtra(EXTRA_EVENT_START, DateFormatters.formatInstant(event.eventStartAt))
            putExtra(EXTRA_EVENT_END, DateFormatters.formatInstant(event.eventEndAt))
            putExtra(EXTRA_EVENT_STATUS, event.status ?: "Upcoming")
            putExtra(EXTRA_EVENT_CAPACITY, event.capacity.toString())
            putExtra(EXTRA_EVENT_COUNT, event.currentAttendeeCount.toString())
        }
        startActivity(intent)
    }

    private fun setupPortalSwitcher() {
        val role = sessionManager.getUserRole() ?: return
        val normalizedRole = RoleMapper.normalizeRole(role)
        val allowedPortals = mutableListOf<String>()
        allowedPortals.add("Attendee Portal")

        if (normalizedRole == AccountRole.STAFF.name || normalizedRole == AccountRole.ADMIN.name || normalizedRole == AccountRole.SUPER_ADMIN.name) {
            allowedPortals.add("Staff Portal")
        }
        if (normalizedRole == AccountRole.ORGANIZER.name || normalizedRole == AccountRole.ADMIN.name || normalizedRole == AccountRole.SUPER_ADMIN.name) {
            allowedPortals.add("Organizer Portal")
        }
        if (normalizedRole == AccountRole.ADMIN.name || normalizedRole == AccountRole.SUPER_ADMIN.name) {
            allowedPortals.add("Admin Portal")
        }

        if (allowedPortals.size > 1) {
            val chip = findViewById<View>(R.id.portalSwitcherChip)
            chip.visibility = View.VISIBLE
            findViewById<View>(R.id.txtDashboardNameDot).visibility = View.VISIBLE
            chip.setOnClickListener {
                showPortalSwitcher(allowedPortals)
            }
        }
    }

    private fun showPortalSwitcher(portals: List<String>) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_portal_switcher, null)

        val container = view.findViewById<LinearLayout>(R.id.portalOptionsContainer)
        portals.forEach { portal ->
            val portalView = layoutInflater.inflate(R.layout.item_portal_option, container, false)
            portalView.findViewById<TextView>(R.id.txtPortalName).text = portal

            val icon = portalView.findViewById<ImageView>(R.id.imgPortalIcon)
            val subtitle = portalView.findViewById<TextView>(R.id.txtPortalSubtitle)

            when (portal) {
                "Attendee Portal" -> {
                    icon.setImageResource(R.drawable.ic_nav_profile)
                    subtitle.text = "Events, rewards, and your profile"
                }
                "Staff Portal" -> {
                    icon.setImageResource(R.drawable.ic_qr_scan)
                    subtitle.text = "Scan QR codes and manage entries"
                }
                "Organizer Portal" -> {
                    icon.setImageResource(R.drawable.ic_nav_calendar)
                    subtitle.text = "Manage your events and attendees"
                }
                "Admin Portal" -> {
                    icon.setImageResource(R.drawable.ic_group)
                    subtitle.text = "Platform administration and oversight"
                }
            }

            if (portal == "Attendee Portal") {
                portalView.findViewById<View>(R.id.currentPortalBadge).visibility = View.VISIBLE
            }

            portalView.setOnClickListener {
                dialog.dismiss()
                switchToPortal(portal)
            }
            container.addView(portalView)
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun switchToPortal(portal: String) {
        when (portal) {
            "Attendee Portal" -> Unit
            "Staff Portal" -> {
                startActivity(Intent(this, com.thedavelopers.eventqr.features.staff.StaffDashboardActivity::class.java))
            }
            "Organizer Portal" -> {
                startActivity(Intent(this, com.thedavelopers.eventqr.features.organizer.dashboard.OrganizerDashboardActivity::class.java))
            }
            "Admin Portal" -> {
                startActivity(Intent(this, com.thedavelopers.eventqr.features.admin.dashboard.AdminDashboardActivity::class.java))
                finish()
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun configureActions() {
        attendeeCard.setOnClickListener {
            startActivity(Intent(this, com.thedavelopers.eventqr.features.attendee.AttendeeEventsActivity::class.java))
        }
        staffCard.setOnClickListener {
            startActivity(Intent(this, com.thedavelopers.eventqr.features.attendee.RegisteredEventsActivity::class.java))
        }
        organizerCard.setOnClickListener {
            startActivity(Intent(this, com.thedavelopers.eventqr.features.attendee.AttendeeTransactionsActivity::class.java))
        }
        notificationsCard.setOnClickListener {
            startActivity(Intent(this, com.thedavelopers.eventqr.features.attendee.RequestEventActivity::class.java))
        }
    }

    private fun stopSwipeRefresh() {
        if (isSwipeRefreshing) {
            swipeRefreshLayout.isRefreshing = false
            isSwipeRefreshing = false
        }
    }
}
