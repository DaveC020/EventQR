package com.thedavelopers.eventqr.features.organizer

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thedavelopers.eventqr.SignIn
import com.thedavelopers.eventqr.core.session.SessionManager
import com.thedavelopers.eventqr.features.organizer.model.dto.OrganizerTransactionRuleDto
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val EXTRA_EVENT_ID = "event_id"
private const val NAV_DASHBOARD = "Dashboard"
private const val NAV_EVENTS = "Events"
private const val NAV_ATTENDEES = "Attendees"
private const val NAV_LOGS = "Logs"
private const val NAV_REPORTS = "Reports"

private val PRIMARY = Color.parseColor("#25215F")
private val PURPLE = Color.parseColor("#5B25C9")
private val BG = Color.parseColor("#F7F7FA")
private val CARD = Color.WHITE
private val TEXT = Color.parseColor("#111827")
private val MUTED = Color.parseColor("#6B7280")
private val BORDER = Color.parseColor("#E5E7EB")
private val SUCCESS = Color.parseColor("#009688")
private val ERROR = Color.parseColor("#EF4444")
private val WARNING = Color.parseColor("#F97316")

private fun AppCompatActivity.dp(value: Int): Int =
    (value * resources.displayMetrics.density).roundToInt()

private fun rounded(
    color: Int,
    radiusDp: Int,
    strokeColor: Int? = BORDER,
    strokeDp: Int = 1,
    density: Float = 1f,
): GradientDrawable = GradientDrawable().apply {
    setColor(color)
    cornerRadius = radiusDp * density
    strokeColor?.let { setStroke((strokeDp * density).roundToInt(), it) }
}

private fun AppCompatActivity.text(
    value: String,
    size: Int = 14,
    bold: Boolean = false,
    color: Int = TEXT,
): TextView = TextView(this).apply {
    text = value
    textSize = size.toFloat()
    setTextColor(color)
    if (bold) setTypeface(typeface, Typeface.BOLD)
    includeFontPadding = true
}

private fun AppCompatActivity.card(padding: Int = 16): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
        background = rounded(CARD, 14, BORDER, density = resources.displayMetrics.density)
        elevation = dp(2).toFloat()
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { setMargins(0, dp(8), 0, dp(10)) }
    }

private fun AppCompatActivity.row(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

private fun AppCompatActivity.section(title: String): TextView =
    text(title, 16, true).apply {
        setPadding(dp(2), dp(16), dp(2), dp(6))
    }

private fun AppCompatActivity.spacer(height: Int): View =
    View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }

private fun AppCompatActivity.primaryButton(label: String, onClick: () -> Unit): Button =
    Button(this).apply {
        text = label
        setAllCaps(false)
        setTextColor(Color.WHITE)
        background = rounded(PURPLE, 8, null, density = resources.displayMetrics.density)
        setOnClickListener { onClick() }
    }

private fun AppCompatActivity.ghostButton(label: String, onClick: () -> Unit): Button =
    Button(this).apply {
        text = label
        setAllCaps(false)
        setTextColor(PRIMARY)
        background = rounded(Color.WHITE, 8, BORDER, density = resources.displayMetrics.density)
        setOnClickListener { onClick() }
    }

private fun AppCompatActivity.chip(label: String, active: Boolean = false, color: Int = PRIMARY): TextView =
    text(label, 12, active, if (active) Color.WHITE else color).apply {
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(7), dp(12), dp(7))
        background = rounded(if (active) color else Color.WHITE, 18, if (active) null else BORDER, density = resources.displayMetrics.density)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { setMargins(0, 0, dp(8), dp(8)) }
    }

private fun AppCompatActivity.badge(value: String): TextView {
    val color = when (value.lowercase()) {
        "approved", "active", "successful", "accepted" -> SUCCESS
        "pending" -> WARNING
        "rejected", "disabled" -> ERROR
        else -> PRIMARY
    }
    return chip(value, false, color).apply {
        background = rounded(color and 0x22FFFFFF or 0x22000000, 18, null, density = resources.displayMetrics.density)
        setTextColor(color)
    }
}

private fun AppCompatActivity.summaryCard(title: String, value: String, accent: Int = PRIMARY): LinearLayout =
    card(14).apply {
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f,
        ).apply { setMargins(dp(4), dp(6), dp(4), dp(6)) }
        addView(text(value, 24, true, TEXT).apply { gravity = Gravity.CENTER })
        addView(text(title, 12, false, MUTED).apply { gravity = Gravity.CENTER })
        addView(View(this@summaryCard).apply {
            background = rounded(accent, 2, null, density = resources.displayMetrics.density)
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(3)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, dp(8), 0, 0)
            }
        })
    }

private fun EditText.afterTextChanged(onChanged: () -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged()
        override fun afterTextChanged(s: Editable?) = Unit
    })
}

private fun AppCompatActivity.selectedEventId(): String =
    intent.getStringExtra(EXTRA_EVENT_ID)
        ?: getSharedPreferences("organizer_mvp_selection", Context.MODE_PRIVATE).getString("selected_event_id", null)
        ?: ""

private fun AppCompatActivity.saveSelectedEventId(eventId: String?) {
    getSharedPreferences("organizer_mvp_selection", Context.MODE_PRIVATE).edit().apply {
        if (eventId.isNullOrBlank()) remove("selected_event_id") else putString("selected_event_id", eventId)
    }.apply()
}

private fun List<OrganizerMvpEvent>.approvedOnly(): List<OrganizerMvpEvent> =
    filter { it.status.equals("Approved", ignoreCase = true) }

private fun AppCompatActivity.resolveSelectedEvent(events: List<OrganizerMvpEvent>): OrganizerMvpEvent? {
    val approved = events.approvedOnly()
    val selected = approved.firstOrNull { it.id == selectedEventId() } ?: approved.firstOrNull()
    saveSelectedEventId(selected?.id)
    return selected
}

private fun AppCompatActivity.openOrganizerPage(target: Class<*>, eventId: String? = null) {
    val intent = Intent(this, target)
    eventId?.let {
        saveSelectedEventId(it)
        intent.putExtra(EXTRA_EVENT_ID, it)
    }
    startActivity(intent)
}

private fun AppCompatActivity.organizerShell(
    title: String,
    subtitle: String? = null,
    selectedNav: String? = null,
    showBack: Boolean = false,
    darkHeader: Boolean = false,
    topRightLabel: String? = null,
    onTopRight: (() -> Unit)? = null,
): LinearLayout {
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(BG)
    }
    setContentView(root)

    val header = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(18), dp(16), dp(if (darkHeader) 22 else 12))
        setBackgroundColor(if (darkHeader) PRIMARY else Color.WHITE)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }
    val headerRow = row()
    if (showBack) {
        headerRow.addView(text("<", 26, false, if (darkHeader) Color.WHITE else TEXT).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
            layoutParams = LinearLayout.LayoutParams(dp(34), dp(42))
        })
    }
    val titleBox = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    titleBox.addView(text(title, 21, true, if (darkHeader) Color.WHITE else TEXT))
    subtitle?.takeIf { it.isNotBlank() }?.let {
        titleBox.addView(text(it, 13, false, if (darkHeader) Color.parseColor("#D7D4F8") else MUTED))
    }
    headerRow.addView(titleBox)
    if (topRightLabel != null) {
        headerRow.addView(ghostButton(topRightLabel) { onTopRight?.invoke() }.apply {
            minWidth = dp(44)
            minHeight = dp(36)
        })
    }
    header.addView(headerRow)
    root.addView(header)

    val scroll = ScrollView(this).apply {
        isFillViewport = false
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f,
        )
    }
    val content = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(18))
    }
    scroll.addView(content)
    root.addView(scroll)

    selectedNav?.let { root.addView(bottomNav(it)) }
    return content
}

private fun AppCompatActivity.bottomNav(selected: String): LinearLayout {
    val nav = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(4), dp(6), dp(4), dp(6))
        setBackgroundColor(Color.WHITE)
        elevation = dp(6).toFloat()
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(68),
        )
    }
    val items = listOf(
        NAV_DASHBOARD to OrganizerDashboardActivity::class.java,
        NAV_EVENTS to ManageEventsActivity::class.java,
        NAV_ATTENDEES to AttendeeManagementActivity::class.java,
        NAV_LOGS to TransactionLogsActivity::class.java,
        NAV_REPORTS to EventReportsActivity::class.java,
    )
    items.forEach { (label, target) ->
        nav.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = if (selected == label) rounded(PRIMARY, 9, null, density = resources.displayMetrics.density) else null
            setPadding(dp(2), dp(4), dp(2), dp(4))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(dp(2), 0, dp(2), 0)
            }
            addView(text(label.take(1), 17, true, if (selected == label) Color.WHITE else TEXT).apply { gravity = Gravity.CENTER })
            addView(text(label, 10, true, if (selected == label) Color.WHITE else TEXT).apply { gravity = Gravity.CENTER })
            setOnClickListener {
                if (this@bottomNav::class.java != target) openOrganizerPage(target, selectedEventId().ifBlank { null })
            }
        })
    }
    return nav
}

private fun AppCompatActivity.eventSelector(
    events: List<OrganizerMvpEvent>,
    selectedId: String?,
    onSelected: (OrganizerMvpEvent) -> Unit,
): Spinner = Spinner(this).apply {
    val approved = events.approvedOnly()
    adapter = ArrayAdapter(
        this@eventSelector,
        android.R.layout.simple_spinner_item,
        approved.map { it.title },
    ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    val selectedIndex = approved.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
    if (approved.isNotEmpty()) setSelection(selectedIndex)
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            approved.getOrNull(position)?.let(onSelected)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }
}

private fun AppCompatActivity.stateCard(): LinearLayout =
    card(12).apply {
        addView(text("State: Success", 13, true, SUCCESS))
        addView(text("Data loaded. Empty, no-result, and error states replace this content when applicable.", 12, false, MUTED))
    }

private fun AppCompatActivity.loadingState(message: String = "Loading..."): LinearLayout =
    card(18).apply {
        gravity = Gravity.CENTER
        addView(text(message, 15, true, MUTED).apply { gravity = Gravity.CENTER })
    }

private fun AppCompatActivity.errorState(message: String, retry: () -> Unit): LinearLayout =
    card(18).apply {
        gravity = Gravity.CENTER
        addView(text("Unable to load live data", 16, true, ERROR).apply { gravity = Gravity.CENTER })
        addView(text(message, 13, false, MUTED).apply { gravity = Gravity.CENTER })
        addView(primaryButton("Retry") { retry() })
    }

private fun AppCompatActivity.dataSourceBanner(load: OrganizerMvpLoad<*>): LinearLayout? =
    if (load.source == OrganizerMvpDataSource.BACKEND) null else card(10).apply {
        elevation = 0f
        background = rounded(Color.parseColor("#FFF7ED"), 10, Color.parseColor("#FED7AA"), density = resources.displayMetrics.density)
        addView(text("Live data temporarily unavailable", 13, true, WARNING))
        addView(text(load.message ?: "Backend request failed. Showing an empty state.", 12, false, MUTED))
    }

private fun AppCompatActivity.emptyState(message: String, button: String? = null, action: (() -> Unit)? = null): LinearLayout =
    card(18).apply {
        gravity = Gravity.CENTER
        addView(text(message, 15, false, MUTED).apply { gravity = Gravity.CENTER })
        if (button != null && action != null) addView(primaryButton(button, action))
    }

open class OrganizerDashboardActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        sessionManager = SessionManager(this)
        showLoadingShell("Loading organizer dashboard...")
        loadDashboard()
    }

    private fun showLoadingShell(message: String) {
        val name = sessionManager.getFullName().orEmpty().ifBlank { "Dharell Dave" }
        val content = organizerShell(
            title = "Organizer Dashboard",
            subtitle = name,
            selectedNav = NAV_DASHBOARD,
            darkHeader = true,
            topRightLabel = "Menu",
        ) { showOrganizerMenu() }
        content.addView(loadingState(message))
    }

    private fun loadDashboard() {
        MainScope().launch {
            val load = repository.loadEventsForMvp()
            renderDashboard(load)
        }
    }

    private fun renderDashboard(load: OrganizerMvpLoad<List<OrganizerMvpEvent>>) {
        val name = sessionManager.getFullName().orEmpty().ifBlank { "Dharell Dave" }
        val events = load.data.approvedOnly()
        val selected = repository.resolveSelectedEvent(load.data, selectedEventId())
        val content = organizerShell(
            title = "Organizer Dashboard",
            subtitle = name,
            selectedNav = NAV_DASHBOARD,
            darkHeader = true,
            topRightLabel = "Menu",
        ) { showOrganizerMenu() }
        dataSourceBanner(load)?.let { content.addView(it) }

        if (selected == null) {
            content.addView(emptyState("No approved event is selected yet.", "Open My Events") {
                openOrganizerPage(ManageEventsActivity::class.java)
            })
            load.message?.let {
                content.addView(errorState(it) { loadDashboard() })
            }
            return
        }

        content.addView(card().apply {
            val top = row()
            top.addView(text("Currently Managing", 14, true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            top.addView(text("Switch Event", 13, false, PRIMARY).apply {
                setOnClickListener { showSwitchEventDialog(events) }
            })
            addView(top)
            addView(spacer(8))
            addView(row().apply {
                val details = LinearLayout(this@OrganizerDashboardActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                details.addView(text(selected.title, 17, true))
                details.addView(text("${selected.shortDate} - ${selected.registeredCount} attendees", 12, false, MUTED))
                addView(details)
                addView(text(">", 24, false, MUTED))
                setOnClickListener { openOrganizerPage(ManageEventsActivity::class.java, selected.id) }
            })
        })

        content.addView(row().apply {
            addView(summaryCard("Registered", selected.registeredCount.toString(), PRIMARY))
            addView(summaryCard("Staff Members", selected.staffCount.toString(), SUCCESS))
        })
        content.addView(row().apply {
            addView(summaryCard("Total Scans", selected.totalTransactions.toString(), WARNING))
            addView(summaryCard("Active Rules", selected.scanPurposesCount.toString(), Color.parseColor("#F59E0B")))
        })

        content.addView(section("Event Management"))
        val actions = listOf(
            "Attendee Management" to AttendeeManagementActivity::class.java,
            "Staff Management" to ManageUsersActivity::class.java,
            "Scan Purposes" to ManageScanPurposesActivity::class.java,
            "Transaction Logs" to TransactionLogsActivity::class.java,
            "Event Reports" to EventReportsActivity::class.java,
            "Event Settings / Manage Event" to ManageEventsActivity::class.java,
        )
        content.addView(card().apply {
            actions.forEach { (label, target) ->
                addView(row().apply {
                    setPadding(0, dp(8), 0, dp(8))
                    addView(text(label, 14, false).apply {
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    addView(text(">", 20, false, MUTED))
                    setOnClickListener { openOrganizerPage(target, selected.id) }
                })
            }
        })
        content.addView(stateCard())
    }

    private fun showSwitchEventDialog(events: List<OrganizerMvpEvent>) {
        if (events.isEmpty()) {
            Toast.makeText(this, "No approved events available.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Switch Event")
            .setItems(events.map { it.title }.toTypedArray()) { _, index ->
                val event = events[index]
                repository.saveSelectedEventId(event.id)
                saveSelectedEventId(event.id)
                Toast.makeText(this, "Now managing ${event.title}", Toast.LENGTH_SHORT).show()
                renderDashboard(OrganizerMvpLoad(events, OrganizerMvpDataSource.MOCK, null))
                loadDashboard()
            }
            .show()
    }

    private fun showOrganizerMenu() {
        AlertDialog.Builder(this)
            .setTitle("Organizer Menu")
            .setItems(arrayOf("Notifications", "Profile", "Switch to Attendee", "Logout")) { _, which ->
                when (which) {
                    0 -> openOrganizerPage(NotificationManagementActivity::class.java)
                    1 -> Toast.makeText(this, "Profile placeholder", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "Switch role placeholder", Toast.LENGTH_SHORT).show()
                    3 -> {
                        sessionManager.clearSession()
                        startActivity(Intent(this, SignIn::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    }
                }
            }
            .show()
    }
}

open class ManageEventsActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var eventList: LinearLayout
    private lateinit var detail: LinearLayout
    private lateinit var search: EditText
    private lateinit var statusSpinner: Spinner
    private var selectedEvent: OrganizerMvpEvent? = null
    private var eventsSource: OrganizerMvpLoad<List<OrganizerMvpEvent>> =
        OrganizerMvpLoad(emptyList(), OrganizerMvpDataSource.MOCK, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        selectedEvent = resolveSelectedEvent(repository.getApprovedOrganizerEvents())
        val content = organizerShell("My Event Requests", selectedNav = NAV_EVENTS)
        content.addView(primaryButton("+ Create New Event Request") {
            // TODO: Connect to the existing event request creation flow when organizer request UX is finalized.
            Toast.makeText(this, "Event request creation is not wired into this screen.", Toast.LENGTH_SHORT).show()
        })
        search = EditText(this).apply {
            hint = "Search events"
            inputType = InputType.TYPE_CLASS_TEXT
            background = rounded(Color.WHITE, 10, BORDER, density = resources.displayMetrics.density)
            setPadding(dp(12), 0, dp(12), 0)
        }
        statusSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@ManageEventsActivity,
                android.R.layout.simple_spinner_item,
                listOf("All", "Approved", "Pending", "Rejected", "Completed"),
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        eventList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        detail = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(search)
        content.addView(statusSpinner)
        content.addView(eventList)
        content.addView(section("Manage Event Detail"))
        content.addView(detail)
        search.afterTextChanged { render() }
        statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = render()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        eventList.addView(loadingState("Loading event requests..."))
        loadEvents()
    }

    private fun loadEvents() {
        MainScope().launch {
            eventsSource = repository.loadEventsForMvp()
            selectedEvent = repository.resolveSelectedEvent(eventsSource.data, selectedEventId())
            render()
        }
    }

    private fun render() {
        val q = search.text.toString()
        val status = statusSpinner.selectedItem?.toString().orEmpty()
        val events = eventsSource.data.filter {
            (status == "All" || it.status.equals(status, true)) &&
                (it.title.contains(q, true) || it.organizerName.contains(q, true))
        }
        eventList.removeAllViews()
        dataSourceBanner(eventsSource)?.let { eventList.addView(it) }
        if (events.isEmpty()) {
            eventList.addView(emptyState("No event requests match your search/filter."))
            detail.removeAllViews()
            return
        }
        events.forEach { event ->
            eventList.addView(eventRequestCard(event))
        }
        val eventToShow = selectedEvent ?: events.firstOrNull { it.status == "Approved" } ?: events.first()
        renderDetail(eventToShow)
    }

    private fun eventRequestCard(event: OrganizerMvpEvent): LinearLayout =
        card().apply {
            val header = row()
            val titleBox = LinearLayout(this@ManageEventsActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            titleBox.addView(text(event.title, 18, true))
            titleBox.addView(text(event.organizerName, 13, false, MUTED))
            titleBox.addView(text("Event Date: ${event.shortDate}\nSubmitted: ${event.submittedDate}", 12, false, MUTED))
            header.addView(titleBox)
            header.addView(badge(event.status))
            addView(header)
            addView(card(10).apply {
                elevation = 0f
                background = rounded(Color.parseColor("#FBF5FF"), 10, null, density = resources.displayMetrics.density)
                addView(text("Additional Organizers (${event.additionalOrganizers.size})", 13, false))
                event.additionalOrganizers.forEach { addView(text(it, 12, false, MUTED)) }
            })
            addView(card(10).apply {
                elevation = 0f
                background = rounded(if (event.status == "Approved") Color.parseColor("#DCFCE7") else Color.parseColor("#FFF7ED"), 10, null, density = resources.displayMetrics.density)
                addView(text("Admin Remarks:", 12, false))
                addView(text(event.adminRemarks, 13, false, if (event.status == "Approved") Color.parseColor("#15803D") else WARNING))
            })
            if (event.status == "Approved") {
                addView(primaryButton("Manage Event") {
                    selectedEvent = event
                    repository.saveSelectedEventId(event.id)
                    saveSelectedEventId(event.id)
                    renderDetail(event)
                })
            }
        }

    private fun renderDetail(event: OrganizerMvpEvent) {
        detail.removeAllViews()
        if (!event.status.equals("Approved", true)) {
            detail.addView(emptyState("Manage Event is available after admin approval."))
            return
        }
        detail.addView(card().apply {
            addView(text(event.title, 18, true))
            addView(text("${event.dateTime}\nVenue: ${event.venue}\nStatus: ${event.status}", 13, false, MUTED))
        })
        detail.addView(row().apply {
            addView(summaryCard("Registered", event.registeredCount.toString()))
            addView(summaryCard("Checked In", event.enteredCount.toString(), SUCCESS))
        })
        detail.addView(row().apply {
            addView(summaryCard("Staff", event.staffCount.toString()))
            addView(summaryCard("Active Purposes", event.scanPurposesCount.toString(), SUCCESS))
        })
        detail.addView(card().apply {
            addView(text("Configuration", 15, true))
            addView(text("ID template: ${event.idTemplateStatus}"))
            addView(text("Rewards: ${event.rewardsStatus}"))
        })
        listOf(
            "Attendee Management" to AttendeeManagementActivity::class.java,
            "Transaction Logs" to TransactionLogsActivity::class.java,
            "Event Reports" to EventReportsActivity::class.java,
            "Manage Staff Access" to ManageUsersActivity::class.java,
            "Scan Purpose Management" to ManageScanPurposesActivity::class.java,
            "ID Template Configuration" to ManageEventsActivity::class.java,
            "Reward Management" to ManageRewardsActivity::class.java,
        ).forEach { (label, target) ->
            detail.addView(ghostButton(label) { openOrganizerPage(target, event.id) })
        }
        detail.addView(stateCard())
    }
}

open class AttendeeManagementActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var selectedEvent: OrganizerMvpEvent
    private lateinit var summary: LinearLayout
    private lateinit var search: EditText
    private lateinit var list: LinearLayout
    private lateinit var detail: LinearLayout
    private var filter = "All"
    private var attendeesSource: OrganizerMvpLoad<List<OrganizerMvpAttendee>> =
        OrganizerMvpLoad(emptyList(), OrganizerMvpDataSource.MOCK, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        selectedEvent = resolveSelectedEvent(repository.getApprovedOrganizerEvents()) ?: return noEvent()
        val content = organizerShell("Attendee Management", selectedEvent.title, NAV_ATTENDEES)
        val approved = repository.getApprovedOrganizerEvents().approvedOnly()
        if (approved.size > 1) content.addView(eventSelector(repository.getApprovedOrganizerEvents(), selectedEvent.id) {
            selectedEvent = it
            repository.saveSelectedEventId(it.id)
            saveSelectedEventId(it.id)
            loadAttendees()
        })
        summary = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(summary)
        search = EditText(this).apply {
            hint = "Search attendee"
            background = rounded(Color.WHITE, 10, BORDER, density = resources.displayMetrics.density)
            setPadding(dp(12), 0, dp(12), 0)
        }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        detail = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(search)
        content.addView(filterChips(listOf("All", "Registered", "Checked In / Entered", "Not Checked In", "Attended", "Exited", "No-show")) {
            filter = it
            render()
        })
        content.addView(list)
        content.addView(section("Attendee Detail"))
        content.addView(detail)
        search.afterTextChanged { render() }
        list.addView(loadingState("Loading attendees..."))
        loadAttendees()
    }

    private fun noEvent() {
        organizerShell("Attendee Management", selectedNav = NAV_ATTENDEES)
            .addView(emptyState("No approved event is selected.", "Open My Events") { openOrganizerPage(ManageEventsActivity::class.java) })
    }

    private fun loadAttendees() {
        MainScope().launch {
            attendeesSource = repository.loadAttendeesForMvp(selectedEvent.id)
            render()
        }
    }

    private fun renderSummary(attendees: List<OrganizerMvpAttendee>) {
        summary.removeAllViews()
        summary.addView(row().apply {
            addView(summaryCard("Total Registered", attendees.size.toString()))
            addView(summaryCard("Checked In", attendees.count { it.currentEventStatus == "Checked In / Entered" }.toString(), SUCCESS))
        })
        summary.addView(row().apply {
            addView(summaryCard("Attended", attendees.count { it.currentEventStatus == "Attended" }.toString(), SUCCESS))
            addView(summaryCard("No-shows", attendees.count { it.currentEventStatus == "No-show" || it.currentEventStatus == "Registered" }.toString(), ERROR))
        })
        dataSourceBanner(attendeesSource)?.let { summary.addView(it) }
    }

    private fun filterChips(labels: List<String>, onClick: (String) -> Unit): LinearLayout =
        row().apply {
            gravity = Gravity.START
            labels.forEach { label ->
                addView(chip(label, label == filter).apply {
                    setOnClickListener { onClick(label) }
                })
            }
        }

    private fun render() {
        val q = search.text.toString()
        val allAttendees = attendeesSource.data
        renderSummary(allAttendees)
        val attendees = allAttendees.filter {
            val matchesStatus = when (filter) {
                "All" -> true
                "Not Checked In" -> it.currentEventStatus == "No-show" || it.currentEventStatus == "Registered"
                else -> it.currentEventStatus.equals(filter, true) || it.registrationStatus.equals(filter, true)
            }
            matchesStatus && (
                it.name.contains(q, true) ||
                    it.email.contains(q, true) ||
                    it.id.contains(q, true) ||
                    it.qrCredentialStatus.contains(q, true)
                )
        }
        list.removeAllViews()
        if (attendees.isEmpty()) {
            list.addView(emptyState("No attendee records found for this view."))
            detail.removeAllViews()
            return
        }
        attendees.forEach { list.addView(attendeeCard(it)) }
        renderDetail(attendees.first())
    }

    private fun attendeeCard(attendee: OrganizerMvpAttendee): LinearLayout =
        card().apply {
            val top = row()
            top.addView(text(attendee.name, 17, true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            top.addView(badge(if (attendee.currentEventStatus == "No-show") "Rejected" else "Accepted"))
            addView(top)
            addView(text(attendee.email, 13, false, MUTED))
            addView(row().apply {
                addView(chip("${attendee.points} pts"))
                addView(chip(attendee.lastTransactionTime, false, SUCCESS))
            })
            addView(text("Registered: ${attendee.registeredDate}", 12, false, MUTED))
            addView(text("Registration: ${attendee.registrationStatus} | Current: ${attendee.currentEventStatus}", 12, false, MUTED))
            setOnClickListener { renderDetail(attendee) }
        }

    private fun renderDetail(attendee: OrganizerMvpAttendee) {
        detail.removeAllViews()
        detail.addView(card().apply {
            addView(text(attendee.name, 18, true))
            addView(text("${attendee.email}\n${attendee.phone}", 13, false, MUTED))
            addView(text("Registration status: ${attendee.registrationStatus}"))
            addView(text("QR credential status: ${attendee.qrCredentialStatus}"))
            addView(text("Current event status: ${attendee.currentEventStatus}"))
            addView(text("Event-specific points: ${attendee.points}"))
            addView(section("Recent Transactions"))
            addView(text(attendee.recentTransactions.ifEmpty { listOf("No recent transactions.") }.joinToString("\n"), 13, false, MUTED))
            addView(section("Recent Rejected Scans"))
            addView(text(attendee.recentRejectedScans.ifEmpty { listOf("No recent rejected scans.") }.joinToString("\n"), 13, false, MUTED))
            addView(ghostButton("View transactions") { openOrganizerPage(TransactionLogsActivity::class.java, selectedEvent.id) })
            addView(ghostButton("Reprint ID") { Toast.makeText(this@AttendeeManagementActivity, "ID reprint flow is not wired on this screen.", Toast.LENGTH_SHORT).show() })
            addView(ghostButton("Manual support note") { Toast.makeText(this@AttendeeManagementActivity, "Support notes are not wired on this screen.", Toast.LENGTH_SHORT).show() })
        })
        detail.addView(stateCard())
    }
}

open class TransactionLogsActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var selectedEvent: OrganizerMvpEvent
    private lateinit var search: EditText
    private lateinit var summary: LinearLayout
    private lateinit var list: LinearLayout
    private lateinit var detail: LinearLayout
    private var filter = "All"
    private var logsSource: OrganizerMvpLoad<List<OrganizerMvpTransaction>> =
        OrganizerMvpLoad(emptyList(), OrganizerMvpDataSource.MOCK, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        selectedEvent = resolveSelectedEvent(repository.getApprovedOrganizerEvents()) ?: return noEvent()
        val content = organizerShell("Transaction Logs", selectedEvent.title, NAV_LOGS, showBack = selectedEventId().isNotBlank())
        if (repository.getApprovedOrganizerEvents().approvedOnly().size > 1) {
            content.addView(eventSelector(repository.getApprovedOrganizerEvents(), selectedEvent.id) {
                selectedEvent = it
                repository.saveSelectedEventId(it.id)
                saveSelectedEventId(it.id)
                loadLogs()
            })
        }
        search = EditText(this).apply {
            hint = "Search attendee, QR ID, transaction ID, staff, or event"
            background = rounded(Color.WHITE, 10, BORDER, density = resources.displayMetrics.density)
            setPadding(dp(12), 0, dp(12), 0)
        }
        summary = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        detail = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(search)
        content.addView(filterChips(listOf("All", "Successful", "Rejected", "Entry", "Attendance", "Benefit Claim", "Booth/Session Visit", "Reward Redemption", "Exit")) {
            filter = it
            render()
        })
        content.addView(summary)
        content.addView(list)
        content.addView(section("Transaction Details"))
        content.addView(detail)
        search.afterTextChanged { render() }
        list.addView(loadingState("Loading transaction logs..."))
        loadLogs()
    }

    private fun noEvent() {
        organizerShell("Transaction Logs", selectedNav = NAV_LOGS)
            .addView(emptyState("No approved event is selected.", "Open My Events") { openOrganizerPage(ManageEventsActivity::class.java) })
    }

    private fun filterChips(labels: List<String>, onClick: (String) -> Unit): LinearLayout =
        row().apply {
            gravity = Gravity.START
            labels.forEach { label ->
                addView(chip(label, label == filter).apply { setOnClickListener { onClick(label) } })
            }
        }

    private fun loadLogs() {
        MainScope().launch {
            logsSource = repository.loadTransactionsForMvp(selectedEvent.id, selectedEvent.title)
            render()
        }
    }

    private fun renderSummary(logs: List<OrganizerMvpTransaction>) {
        summary.removeAllViews()
        summary.addView(row().apply {
            addView(summaryCard("Total Scans", logs.size.toString()))
            addView(summaryCard("Successful", logs.count { it.status == "Successful" }.toString(), SUCCESS))
            addView(summaryCard("Rejected", logs.count { it.status == "Rejected" }.toString(), ERROR))
        })
        dataSourceBanner(logsSource)?.let { summary.addView(it) }
    }

    private fun render() {
        val q = search.text.toString()
        val allLogs = logsSource.data
        renderSummary(allLogs)
        val logs = allLogs.filter {
            val matchesFilter = filter == "All" ||
                it.status.equals(filter, true) ||
                it.type.equals(filter, true) ||
                (filter == "Booth/Session Visit" && it.type.contains("Visit", true))
            val matchesQuery = listOf(it.attendeeName, it.qrId, it.id, it.staffName, it.eventTitle).any { value ->
                value.contains(q, true)
            }
            matchesFilter && matchesQuery
        }
        list.removeAllViews()
        if (logs.isEmpty()) {
            list.addView(emptyState("No transaction logs match this view."))
            detail.removeAllViews()
            return
        }
        logs.forEach { list.addView(logCard(it)) }
        renderDetail(logs.first())
    }

    private fun logCard(log: OrganizerMvpTransaction): LinearLayout =
        card().apply {
            val top = row()
            top.addView(text(log.attendeeName, 16, true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            top.addView(badge(if (log.status == "Successful") "Accepted" else "Rejected"))
            addView(top)
            addView(text(log.eventTitle, 12, false, MUTED))
            addView(text("${log.id} | ${log.scanPurpose}", 12, false, MUTED))
            addView(text(log.timestamp, 12, false, MUTED))
            addView(text("Staff: ${log.staffName} (${log.staffId})", 12, false, MUTED))
            if (log.status == "Rejected") addView(text("Reason: ${log.reason}", 12, true, ERROR))
            addView(text(log.message, 13, false, if (log.status == "Rejected") ERROR else SUCCESS))
            setOnClickListener { renderDetail(log) }
        }

    private fun renderDetail(log: OrganizerMvpTransaction) {
        detail.removeAllViews()
        detail.addView(card().apply {
            addView(text("Transaction ID: ${log.id}", 15, true))
            addView(text("Event: ${log.eventTitle} / ${log.eventId}"))
            addView(text("Attendee: ${log.attendeeName} / ${log.attendeeId}"))
            addView(text("Staff: ${log.staffName} / ${log.staffId}"))
            addView(text("QR ID: ${log.qrId}"))
            addView(text("Scan purpose: ${log.scanPurpose}"))
            addView(text("Result status: ${log.status}"))
            addView(text("Reason/message: ${log.reason}"))
            addView(text("Created timestamp: ${log.timestamp}"))
            addView(text("Device/source: ${log.deviceSource}"))
            addView(text("Points awarded/deducted: ${log.pointsDelta}"))
            addView(text("Related reward/benefit/session: ${log.relatedItem}"))
        })
        detail.addView(stateCard())
    }
}

open class EventReportsActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var selectedEvent: OrganizerMvpEvent
    private lateinit var report: LinearLayout
    private var reportSource: OrganizerMvpLoad<OrganizerMvpEvent>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        selectedEvent = resolveSelectedEvent(repository.getApprovedOrganizerEvents()) ?: return noEvent()
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

    private fun noEvent() {
        organizerShell("Event Reports", selectedNav = NAV_REPORTS)
            .addView(emptyState("No approved event is selected.", "Open My Events") { openOrganizerPage(ManageEventsActivity::class.java) })
    }

    private fun render() {
        val reportEvent = reportSource?.data ?: selectedEvent
        report.removeAllViews()
        reportSource?.let { dataSourceBanner(it)?.let { banner -> report.addView(banner) } }
        report.addView(row().apply {
            addView(summaryCard("Total Attendance", reportEvent.registeredCount.toString(), Color.parseColor("#3B82F6")))
            addView(summaryCard("Checked In", reportEvent.enteredCount.toString(), SUCCESS))
        })
        report.addView(row().apply {
            addView(summaryCard("No Shows", reportEvent.noShowCount.toString(), ERROR))
            addView(summaryCard("Points Distributed", reportEvent.totalPointsAwarded.toString(), Color.parseColor("#F59E0B")))
        })
        report.addView(row().apply {
            addView(summaryCard("Benefit Claims", reportEvent.benefitClaims.toString(), PURPLE))
            addView(summaryCard("Booth Visits", reportEvent.boothSessionVisits.toString(), PRIMARY))
        })
        report.addView(row().apply {
            addView(summaryCard("Reward Redemptions", reportEvent.rewardRedemptions.toString(), SUCCESS))
            addView(summaryCard("Rejected Scans", reportEvent.rejectedScans.toString(), ERROR))
        })
        report.addView(reportSection("QR Transaction Summary", listOf(
            "Entry Scans" to reportEvent.enteredCount.toString(),
            "Exit Scans" to reportEvent.exitedCount.toString(),
            "Booth Visits" to reportEvent.boothSessionVisits.toString(),
            "Attendance Scans" to reportEvent.attendedCount.toString(),
            "Benefit Claims" to reportEvent.benefitClaims.toString(),
            "Reward Redemptions" to reportEvent.rewardRedemptions.toString(),
        )))
        report.addView(reportSection("Attendance Summary", listOf(
            "Registered" to reportEvent.registeredCount.toString(),
            "Checked In / Entered" to reportEvent.enteredCount.toString(),
            "Attended" to reportEvent.attendedCount.toString(),
            "No Shows" to reportEvent.noShowCount.toString(),
        )))
        report.addView(reportSection("Rejected Transaction Summary", listOf(
            "Rejected scans" to reportEvent.rejectedScans.toString(),
            "Wrong event QR" to "1",
            "Duplicate claim rejected" to "2",
        )))
        report.addView(reportSection("Points and Rewards Summary", listOf(
            "Points distributed" to reportEvent.totalPointsAwarded.toString(),
            "Reward redemptions" to reportEvent.rewardRedemptions.toString(),
            "Rewards status" to reportEvent.rewardsStatus,
        )))
        val recent = repository.getOrganizerTransactions(reportEvent.id).take(4).map { it.message to it.timestamp }
        report.addView(reportSection("Recent Activity", recent.ifEmpty { listOf("No recent activity" to "-") }))
        report.addView(stateCard())
    }

    private fun loadReport() {
        report.removeAllViews()
        report.addView(loadingState("Loading report..."))
        MainScope().launch {
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

open class ManageUsersActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var selectedEvent: OrganizerMvpEvent
    private lateinit var search: EditText
    private lateinit var results: LinearLayout
    private lateinit var assigned: LinearLayout
    private val assignedStaff = mutableListOf<OrganizerMvpStaff>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        selectedEvent = resolveSelectedEvent(repository.getApprovedOrganizerEvents()) ?: return noEvent()
        val content = organizerShell("Staff Management", selectedEvent.title, showBack = true, topRightLabel = "+") {
            renderSearch(showOnlyWhenQuery = false)
        }
        content.addView(card().apply {
            addView(text("Total Staff", 12, false, MUTED))
            addView(text(selectedEvent.staffCount.toString(), 24, true))
        })
        content.addView(primaryButton("+ Add Staff Member") { renderSearch(showOnlyWhenQuery = false) })
        search = EditText(this).apply {
            hint = "Search user by email/name"
            background = rounded(Color.WHITE, 10, BORDER, density = resources.displayMetrics.density)
            setPadding(dp(12), 0, dp(12), 0)
        }
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        assigned = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(search)
        content.addView(section("Search Results"))
        content.addView(results)
        content.addView(section("Staff Members"))
        content.addView(assigned)
        search.afterTextChanged { renderSearch() }
        renderSearch()
        loadAssigned()
    }

    private fun noEvent() {
        organizerShell("Staff Management", showBack = true)
            .addView(emptyState("No approved event is selected.", "Open My Events") { openOrganizerPage(ManageEventsActivity::class.java) })
    }

    private fun renderSearch(showOnlyWhenQuery: Boolean = true) {
        if (!::results.isInitialized) return
        val query = search.text.toString()
        results.removeAllViews()
        if (showOnlyWhenQuery && query.isBlank()) {
            results.addView(text("Search by name or email to add staff.", 13, false, MUTED))
            return
        }
        results.addView(loadingState("Searching users..."))
        MainScope().launch {
            val source = repository.searchStaffUsersForMvp(query)
            results.removeAllViews()
            dataSourceBanner(source)?.let { results.addView(it) }
            if (source.data.isEmpty()) {
                results.addView(text("User not found.", 13, true, ERROR))
                return@launch
            }
            source.data.forEach { user ->
                results.addView(staffCard(user.copy(assignedEventId = selectedEvent.id, assignedEvent = selectedEvent.title), true))
            }
        }
    }

    private fun renderAssigned() {
        assigned.removeAllViews()
        val staff = assignedStaff.filter { it.assignedEventId == selectedEvent.id }
        if (staff.isEmpty()) {
            assigned.addView(emptyState("Empty staff list. Add staff members before event day."))
            return
        }
        staff.forEach { assigned.addView(staffCard(it, false)) }
        assigned.addView(stateCard())
    }

    private fun loadAssigned() {
        assigned.removeAllViews()
        assigned.addView(loadingState("Loading staff..."))
        MainScope().launch {
            val source = repository.loadStaffForMvp(selectedEvent)
            source.message?.let {
                Toast.makeText(this@ManageUsersActivity, it, Toast.LENGTH_SHORT).show()
            }
            renderAssigned()
        }
    }

    private fun staffCard(staff: OrganizerMvpStaff, canAdd: Boolean): LinearLayout =
        card().apply {
            val top = row()
            top.addView(text(staff.name, 17, true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            top.addView(badge(staff.accessStatus))
            addView(top)
            addView(chip(staff.roleLabel))
            addView(text(staff.email, 13, false, MUTED))
            addView(text("Permissions: ${staff.permissions.joinToString(", ")}", 12, false, MUTED))
            addView(text("Added: ${staff.addedDate}", 12, false, MUTED))
            if (canAdd) {
                addView(primaryButton("Add staff to event") {
                    if (assignedStaff.any { it.email.equals(staff.email, ignoreCase = true) && it.accessStatus.equals("Active", ignoreCase = true) }) {
                        Toast.makeText(this@ManageUsersActivity, "Duplicate staff assignment", Toast.LENGTH_SHORT).show()
                    } else {
                        MainScope().launch {
                            val source = repository.addStaffForMvp(selectedEvent, staff)
                            assignedStaff.add(source.data)
                            source.message?.let {
                                Toast.makeText(this@ManageUsersActivity, it, Toast.LENGTH_SHORT).show()
                            }
                            renderAssigned()
                        }
                    }
                })
            } else {
                addView(ghostButton("Enable/disable staff access") {
                    val index = assignedStaff.indexOfFirst { it.id == staff.id && it.assignedEventId == staff.assignedEventId }
                    if (index >= 0) {
                        val nextStatus = if (assignedStaff[index].accessStatus == "Active") "Disabled" else "Active"
                        val updated = assignedStaff[index].copy(accessStatus = nextStatus)
                        MainScope().launch {
                            val source = repository.updateStaffForMvp(selectedEvent, updated)
                            assignedStaff[index] = source.data
                            source.message?.let {
                                Toast.makeText(this@ManageUsersActivity, it, Toast.LENGTH_SHORT).show()
                            }
                            renderAssigned()
                        }
                    }
                })
                addView(ghostButton("View/edit staff permissions") {
                    AlertDialog.Builder(this@ManageUsersActivity)
                        .setTitle(staff.name)
                        .setMessage("Role: ${staff.roleLabel}\nPermissions: ${staff.permissions.joinToString(", ")}")
                        .setPositiveButton("Close", null)
                        .show()
                })
                addView(ghostButton("Remove staff from event") {
                    AlertDialog.Builder(this@ManageUsersActivity)
                        .setTitle("Remove staff?")
                        .setMessage("Remove ${staff.name} from ${staff.assignedEvent}?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove") { _, _ ->
                            MainScope().launch {
                                val source = repository.removeStaffForMvp(selectedEvent, staff)
                                assignedStaff.removeAll { it.id == staff.id && it.assignedEventId == staff.assignedEventId }
                                source.message?.let {
                                    Toast.makeText(this@ManageUsersActivity, it, Toast.LENGTH_SHORT).show()
                                }
                                renderAssigned()
                            }
                        }
                        .show()
                })
            }
        }
}

open class ManageScanPurposesActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var selectedEvent: OrganizerMvpEvent
    private lateinit var summaryHost: LinearLayout
    private lateinit var rulesHost: LinearLayout
    private lateinit var purposeHost: LinearLayout
    private val purposeInputs = mutableListOf<Pair<OrganizerMvpScanPurpose, LinearLayout>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        selectedEvent = resolveSelectedEvent(repository.getApprovedOrganizerEvents()) ?: return noEvent()
        val content = organizerShell("Scan Purposes", selectedEvent.title, showBack = true)
        content.addView(card().apply {
            background = rounded(Color.parseColor("#E5E7EB"), 12, Color.parseColor("#C7CAD1"), density = resources.displayMetrics.density)
            addView(text("Configure which scan purposes are available for your event. Active scan purposes will be available to staff during QR scanning.", 14, false))
        })
        summaryHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        rulesHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        purposeHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(summaryHost)
        content.addView(rulesHost)
        content.addView(purposeHost)
        content.addView(section("Transaction Rules"))
        listOf(
            "Prevent duplicate entry",
            "Prevent duplicate attendance if configured",
            "Prevent duplicate benefit claim",
            "Prevent duplicate reward claim",
            "Reject wrong event QR",
            "Reject inactive/invalid registration",
            "Reject unauthorized staff scan",
            "Tracking-only purposes log transactions without awarding points",
            "Point-enabled purposes award event-specific points after valid scans",
        ).forEach {
            content.addView(CheckBox(this).apply { text = it; isChecked = true })
        }
        content.addView(primaryButton("Add/Edit scan purpose rule") {
            Toast.makeText(this, "Edit fields directly in each purpose card.", Toast.LENGTH_SHORT).show()
        })
        content.addView(primaryButton("Save configuration") { validateAndSave() })
        content.addView(ghostButton("Reset / Cancel changes") { recreate() })
        content.addView(ghostButton("Configure points") {
            Toast.makeText(this, "Points configuration is handled per scan purpose.", Toast.LENGTH_SHORT).show()
        })
        content.addView(stateCard())
        loadPurposes()
    }

    private fun noEvent() {
        organizerShell("Scan Purposes", showBack = true)
            .addView(emptyState("No approved event is selected.", "Open My Events") { openOrganizerPage(ManageEventsActivity::class.java) })
    }

    private fun loadPurposes() {
        summaryHost.removeAllViews()
        purposeHost.removeAllViews()
        purposeInputs.clear()
        purposeHost.addView(loadingState("Loading scan purposes..."))
        MainScope().launch {
            val source = repository.loadScanPurposesForMvp(selectedEvent.id)
            val rulesSource = repository.loadTransactionRulesForMvp(selectedEvent.id)
            renderPurposes(source.data, rulesSource.data)
            source.message?.let {
                Toast.makeText(this@ManageScanPurposesActivity, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderPurposes(purposes: List<OrganizerMvpScanPurpose>, rules: List<OrganizerTransactionRuleDto>) {
        summaryHost.removeAllViews()
        rulesHost.removeAllViews()
        purposeHost.removeAllViews()
        summaryHost.addView(row().apply {
            addView(summaryCard("Active Purposes", purposes.count { it.enabled }.toString()))
            addView(summaryCard("With Points", purposes.count { it.pointsEnabled }.toString(), SUCCESS))
        })
        rulesHost.addView(card().apply {
            addView(text("Transaction Rules", 16, true))
            addView(text("Loaded from the backend for this event.", 12, false, MUTED))
            addView(text("Configured rules: ${rules.size}", 13, true, PRIMARY))
            if (rules.isEmpty()) {
                addView(text("No transaction rules configured yet.", 12, false, MUTED))
            } else {
                rules.forEach { rule ->
                    addView(text(
                        "${rule.scanPurposeId} | active=${rule.active} | duplicate=${rule.allowDuplicate} | staff=${rule.requiresStaffAssignment} | points=${rule.pointsAwarded}",
                        12,
                        false,
                        MUTED,
                    ))
                }
            }
        })
        val header = row()
        header.addView(text("Available Purposes", 14, true, MUTED).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(text("Configure Points", 14, true, PRIMARY))
        purposeHost.addView(header)
        purposes.forEach { purpose ->
            val view = purposeCard(purpose)
            purposeInputs.add(purpose to view)
            purposeHost.addView(view)
        }
    }

    private fun purposeCard(purpose: OrganizerMvpScanPurpose): LinearLayout =
        card().apply {
            val top = row()
            top.addView(text(purpose.label, 16, true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            top.addView(CheckBox(this@ManageScanPurposesActivity).apply {
                text = if (purpose.enabled) "Enabled" else "Disabled"
                isChecked = purpose.enabled
                setOnCheckedChangeListener { button, checked ->
                    button.text = if (checked) "Enabled" else "Disabled"
                }
            })
            addView(top)
            addView(text(purpose.description, 12, false, MUTED))
            addView(CheckBox(this@ManageScanPurposesActivity).apply {
                text = "Tracking only"
                isChecked = purpose.trackingOnly
            })
            addView(CheckBox(this@ManageScanPurposesActivity).apply {
                text = "Points enabled"
                isChecked = purpose.pointsEnabled
            })
            addView(EditText(this@ManageScanPurposesActivity).apply {
                hint = "Points value"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                setText(purpose.pointsValue.toString())
                background = rounded(Color.WHITE, 8, BORDER, density = resources.displayMetrics.density)
                setPadding(dp(10), 0, dp(10), 0)
            })
            addView(EditText(this@ManageScanPurposesActivity).apply {
                hint = "Duplicate rule"
                setText(purpose.duplicateRule)
                background = rounded(Color.WHITE, 8, BORDER, density = resources.displayMetrics.density)
                setPadding(dp(10), 0, dp(10), 0)
            })
            addView(EditText(this@ManageScanPurposesActivity).apply {
                hint = "Required selection label"
                setText(purpose.requiredSelectionLabel)
                background = rounded(Color.WHITE, 8, BORDER, density = resources.displayMetrics.density)
                setPadding(dp(10), 0, dp(10), 0)
            })
            addView(text("Badges: ${if (purpose.trackingOnly) "Tracking Only" else "Transactions"}${if (purpose.pointsEnabled) " | Points Enabled (${purpose.pointsValue})" else ""}", 12, false, MUTED))
        }

    private fun validateAndSave() {
        val errors = mutableListOf<String>()
        val updated = mutableListOf<OrganizerMvpScanPurpose>()
        purposeInputs.forEach { (purpose, view) ->
            val enabled = (((view.getChildAt(0) as LinearLayout).getChildAt(1)) as CheckBox).isChecked
            val trackingOnly = (view.getChildAt(2) as CheckBox).isChecked
            val pointsEnabled = (view.getChildAt(3) as CheckBox).isChecked
            val points = (view.getChildAt(4) as EditText).text.toString().toIntOrNull()
            val duplicateRule = (view.getChildAt(5) as EditText).text.toString()
            val requiredSelection = (view.getChildAt(6) as EditText).text.toString()
            if (points == null || points < 0) errors.add("${purpose.label}: invalid point value")
            if (trackingOnly && pointsEnabled) errors.add("${purpose.label}: conflicting tracking-only and points rules")
            updated.add(
                purpose.copy(
                    enabled = enabled,
                    trackingOnly = trackingOnly,
                    pointsEnabled = pointsEnabled,
                    pointsValue = points ?: 0,
                    duplicateRule = duplicateRule,
                    requiredSelectionLabel = requiredSelection,
                )
            )
        }
        if (purposeInputs.none { it.first.label == "Entrance Logging" || it.first.label == "Entry" }) {
            errors.add("Missing required scan purpose: Entrance Logging / Entry")
        }
        if (errors.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Validation errors")
                .setMessage(errors.joinToString("\n"))
                .setPositiveButton("OK", null)
                .show()
            return
        }
        MainScope().launch {
            val source = repository.saveScanPurposesForMvp(selectedEvent.id, updated)
            val rulesSource = repository.loadTransactionRulesForMvp(selectedEvent.id)
            source.message?.let {
                Toast.makeText(this@ManageScanPurposesActivity, it, Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this@ManageScanPurposesActivity, "Configuration saved", Toast.LENGTH_SHORT).show()
            renderPurposes(source.data, rulesSource.data)
        }
    }
}

open class AttendeeDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val attendeeId = intent.getStringExtra("extra_attendee_id").orEmpty()
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()
        val content = organizerShell("Attendee Details", showBack = true)
        if (attendeeId.isBlank() || eventId.isBlank()) {
            content.addView(emptyState("Open attendee details from an event to view live records."))
            return
        }
        content.addView(loadingState("Loading attendee details..."))
        MainScope().launch {
            val attendeeLoad = OrganizerRepository(this@AttendeeDetailsActivity).loadAttendeesForMvp(eventId)
            val attendee = attendeeLoad.data.firstOrNull { it.id == attendeeId }
            content.removeAllViews()
            attendeeLoad.message?.let { content.addView(errorState(it) { recreate() }) }
            if (attendee == null) {
                content.addView(emptyState("Attendee record not found for this event."))
                return@launch
            }
            content.addView(card().apply {
                addView(text(attendee.name, 18, true))
                addView(text("${attendee.email}\n${attendee.phone}\nStatus: ${attendee.currentEventStatus}\nPoints: ${attendee.points}", 13, false, MUTED))
            })
        }
    }
}

open class ReportsActivity : EventReportsActivity()

open class ManageRewardsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        organizerShell("Reward Management", "Existing organizer reward page placeholder.", showBack = true)
            .addView(emptyState("Reward management is not configured for this event yet."))
    }
}

open class NotificationManagementActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        organizerShell("Notifications", "Organizer notification placeholder.", showBack = true)
            .addView(emptyState("Notifications are not configured for this event yet."))
    }
}
