package com.thedavelopers.eventqr.features.organizer

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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
import kotlin.math.roundToInt

private fun AppCompatActivity.dp(value: Int): Int =
    (value * resources.displayMetrics.density).roundToInt()

private fun AppCompatActivity.mvpPage(
    title: String,
    subtitle: String,
    showBack: Boolean = true,
): LinearLayout {
    val scrollView = ScrollView(this)
    val content = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(24))
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }
    scrollView.addView(content)
    setContentView(scrollView)

    if (showBack) {
        content.addView(Button(this).apply {
            text = "Back"
            setOnClickListener { finish() }
        })
    }
    content.addView(label(title, 24, true))
    content.addView(label(subtitle, 14, false, "#5F6368"))
    content.addView(spacer(12))
    return content
}

private fun AppCompatActivity.label(
    textValue: String,
    sizeSp: Int = 16,
    bold: Boolean = false,
    color: String = "#202124",
): TextView = TextView(this).apply {
    text = textValue
    textSize = sizeSp.toFloat()
    setTextColor(Color.parseColor(color))
    if (bold) setTypeface(typeface, Typeface.BOLD)
    setPadding(0, dp(4), 0, dp(4))
}

private fun AppCompatActivity.spacer(heightDp: Int): View =
    View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
    }

private fun AppCompatActivity.section(title: String): TextView =
    label(title, 18, true).apply { setPadding(0, dp(18), 0, dp(6)) }

private fun AppCompatActivity.card(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setBackgroundResource(com.thedavelopers.eventqr.R.drawable.bg_card)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { setMargins(0, dp(6), 0, dp(8)) }
    }

private fun AppCompatActivity.primaryButton(textValue: String, onClick: () -> Unit): Button =
    Button(this).apply {
        text = textValue
        setAllCaps(false)
        setOnClickListener { onClick() }
    }

private fun AppCompatActivity.statCard(title: String, value: String): LinearLayout =
    card().apply {
        addView(label(value, 22, true))
        addView(label(title, 13, false, "#5F6368"))
    }

private fun AppCompatActivity.stateBox(vararg states: String): LinearLayout =
    card().apply {
        addView(label("Screen states", 15, true))
        states.forEach { addView(label(it, 13, false, "#5F6368")) }
    }

private fun AppCompatActivity.eventSelector(
    events: List<OrganizerMvpEvent>,
    onSelected: (OrganizerMvpEvent) -> Unit,
): Spinner = Spinner(this).apply {
    adapter = ArrayAdapter(
        this@eventSelector,
        android.R.layout.simple_spinner_item,
        events.map { it.title },
    ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            onSelected(events[position])
        }

        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }
}

private fun EditText.afterTextChanged(onChanged: () -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged()
        override fun afterTextChanged(s: Editable?) = Unit
    })
}

private fun LinearLayout.replaceWith(items: List<View>) {
    removeAllViews()
    items.forEach { addView(it) }
}

open class OrganizerDashboardActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        val events = repository.getApprovedOrganizerEvents()
        val content = mvpPage(
            title = "Organizer Dashboard",
            subtitle = "Hello, Organizer. Manage approved EventQR events, attendee flow, scans, staff, and reports.",
            showBack = false,
        )

        val totals = events.fold(OrganizerTotals()) { acc, event ->
            acc.copy(
                events = acc.events + 1,
                registered = acc.registered + event.registeredCount,
                entered = acc.entered + event.enteredCount,
                transactions = acc.transactions + event.totalTransactions,
                issues = acc.issues + event.rejectedScans,
            )
        }

        content.addView(section("Summary"))
        listOf(
            "Approved events count" to totals.events.toString(),
            "Total registered attendees" to totals.registered.toString(),
            "Total checked-in/entered attendees" to totals.entered.toString(),
            "Total transactions" to totals.transactions.toString(),
            "Pending issues/rejected scans" to totals.issues.toString(),
        ).forEach { content.addView(statCard(it.first, it.second)) }

        content.addView(section("My Approved Events"))
        if (events.isEmpty()) {
            content.addView(label("Empty state: no approved events assigned to this organizer yet."))
        } else {
            events.take(3).forEach { event ->
                content.addView(card().apply {
                    addView(label(event.title, 17, true))
                    addView(label("${event.dateTime} | ${event.venue}"))
                    addView(label("Registered ${event.registeredCount} | Entered ${event.enteredCount} | Status ${event.status}"))
                    addView(primaryButton("Manage Event") {
                        startActivity(Intent(this@OrganizerDashboardActivity, ManageEventsActivity::class.java))
                    })
                })
            }
        }

        content.addView(section("Quick Actions"))
        listOf(
            "My Events / Event Management" to ManageEventsActivity::class.java,
            "Attendee Management" to AttendeeManagementActivity::class.java,
            "Transaction Logs" to TransactionLogsActivity::class.java,
            "Event Reports" to EventReportsActivity::class.java,
            "Manage Staff Access" to ManageUsersActivity::class.java,
            "Scan Purpose Management" to ManageScanPurposesActivity::class.java,
        ).forEach { (title, activityClass) ->
            content.addView(primaryButton(title) {
                startActivity(Intent(this, activityClass))
            })
        }

        content.addView(stateBox(
            "Loading: shown while organizer metrics load.",
            "Success: sample approved events are displayed.",
            "Empty: dashboard shows a no-events message.",
            "Error: backend failures will surface here after integration.",
        ))
    }
}

private data class OrganizerTotals(
    val events: Int = 0,
    val registered: Int = 0,
    val entered: Int = 0,
    val transactions: Int = 0,
    val issues: Int = 0,
)

open class ManageEventsActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var listContainer: LinearLayout
    private lateinit var detailsContainer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var statusFilter: Spinner
    private var selectedEvent: OrganizerMvpEvent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        val content = mvpPage(
            title = "My Events / Event Management",
            subtitle = "Approved organizer events only. Event creation/review remains outside this organizer MVP screen.",
        )
        searchInput = EditText(this).apply {
            hint = "Search approved events"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        statusFilter = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@ManageEventsActivity,
                android.R.layout.simple_spinner_item,
                listOf("All statuses", "Approved", "Active", "Ended"),
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        detailsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        content.addView(searchInput)
        content.addView(statusFilter)
        content.addView(stateBox(
            "Loading: event list loading.",
            "Empty: no approved events.",
            "Success: approved events shown below.",
            "Error: failed to load organizer events.",
        ))
        content.addView(section("Approved Events"))
        content.addView(listContainer)
        content.addView(section("Event Management Details"))
        content.addView(detailsContainer)

        searchInput.afterTextChanged { render() }
        statusFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = render()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        render()
    }

    private fun render() {
        val query = searchInput.text.toString()
        val status = statusFilter.selectedItem?.toString().orEmpty()
        val events = repository.getApprovedOrganizerEvents().filter {
            it.title.contains(query, ignoreCase = true) &&
                (status == "All statuses" || it.status.equals(status, ignoreCase = true))
        }
        listContainer.removeAllViews()
        if (events.isEmpty()) {
            listContainer.addView(label("Empty/no-result state: no approved events match this view."))
            detailsContainer.removeAllViews()
            return
        }
        events.forEach { event ->
            listContainer.addView(card().apply {
                addView(label(event.title, 17, true))
                addView(label("Date/time: ${event.dateTime}"))
                addView(label("Venue: ${event.venue}"))
                addView(label("Status: ${event.status}"))
                addView(label("Registered: ${event.registeredCount} | Checked-in/entered: ${event.enteredCount}"))
                addView(primaryButton("Manage Event") {
                    selectedEvent = event
                    renderDetails(event)
                })
            })
        }
        if (selectedEvent == null) {
            selectedEvent = events.first()
            renderDetails(events.first())
        }
    }

    private fun renderDetails(event: OrganizerMvpEvent) {
        detailsContainer.removeAllViews()
        detailsContainer.addView(card().apply {
            addView(label("Event information", 16, true))
            addView(label("${event.title}\n${event.dateTime}\n${event.venue}\nStatus: ${event.status}"))
        })
        detailsContainer.addView(card().apply {
            addView(label("Registration/attendance summary", 16, true))
            addView(label("Registered: ${event.registeredCount}"))
            addView(label("Entered / checked-in: ${event.enteredCount}"))
            addView(label("Attended: ${event.attendedCount}"))
            addView(label("Exited: ${event.exitedCount}"))
            addView(label("No-show: ${event.noShowCount}"))
        })
        detailsContainer.addView(card().apply {
            addView(label("Configuration status", 16, true))
            addView(label("ID template status: ${event.idTemplateStatus}"))
            addView(label("Rewards status: ${event.rewardsStatus}"))
            addView(label("Staff count: ${event.staffCount}"))
            addView(label("Scan purposes count: ${event.scanPurposesCount}"))
        })
        listOf(
            "Attendee Management" to AttendeeManagementActivity::class.java,
            "Transaction Logs" to TransactionLogsActivity::class.java,
            "Event Reports" to EventReportsActivity::class.java,
            "Staff Access" to ManageUsersActivity::class.java,
            "Scan Purpose Settings" to ManageScanPurposesActivity::class.java,
        ).forEach { (title, activityClass) ->
            detailsContainer.addView(primaryButton(title) {
                startActivity(Intent(this, activityClass).putExtra("event_id", event.id))
            })
        }
    }
}

open class AttendeeManagementActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var selector: Spinner
    private lateinit var searchInput: EditText
    private lateinit var statusFilter: Spinner
    private lateinit var attendeeList: LinearLayout
    private lateinit var details: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        val events = repository.getApprovedOrganizerEvents()
        val content = mvpPage(
            title = "Attendee Management",
            subtitle = "View attendee status, QR credential status, points, and event transaction history.",
        )
        selector = eventSelector(events) { render() }
        searchInput = EditText(this).apply { hint = "Search attendees by name or email" }
        statusFilter = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@AttendeeManagementActivity,
                android.R.layout.simple_spinner_item,
                listOf("All", "Registered", "Entered / Checked-in", "Attended", "Exited", "No-show"),
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        attendeeList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        content.addView(label("Event selector", 15, true))
        content.addView(selector)
        content.addView(searchInput)
        content.addView(statusFilter)
        content.addView(stateBox(
            "Loading: attendee records loading.",
            "Empty: selected event has no attendees.",
            "No-result: filters/search matched nobody.",
            "Success: attendees shown below.",
            "Error: failed to load attendees.",
        ))
        content.addView(section("Attendee List"))
        content.addView(attendeeList)
        content.addView(section("Attendee Details"))
        content.addView(details)

        searchInput.afterTextChanged { render() }
        statusFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = render()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        render()
    }

    private fun selectedEvent(): OrganizerMvpEvent =
        repository.getApprovedOrganizerEvents()[selector.selectedItemPosition.coerceAtLeast(0)]

    private fun render() {
        if (!::selector.isInitialized || !::attendeeList.isInitialized) return
        val event = selectedEvent()
        val query = searchInput.text.toString()
        val filter = statusFilter.selectedItem?.toString().orEmpty()
        val attendees = repository.getOrganizerAttendees(event.id).filter {
            (filter == "All" || it.currentEventStatus.equals(filter, true) || it.registrationStatus.equals(filter, true)) &&
                (it.name.contains(query, true) || it.email.contains(query, true))
        }
        attendeeList.removeAllViews()
        if (attendees.isEmpty()) {
            attendeeList.addView(label("Empty/no-result state: no attendee records found for ${event.title}."))
            details.removeAllViews()
            return
        }
        attendees.forEach { attendee ->
            attendeeList.addView(card().apply {
                addView(label(attendee.name, 17, true))
                addView(label(attendee.email))
                addView(label("Registration: ${attendee.registrationStatus} | Current: ${attendee.currentEventStatus}"))
                addView(label("Points: ${attendee.points} | Last transaction: ${attendee.lastTransactionTime}"))
                addView(primaryButton("View Details") { renderDetails(attendee) })
            })
        }
        renderDetails(attendees.first())
    }

    private fun renderDetails(attendee: OrganizerMvpAttendee) {
        details.removeAllViews()
        details.addView(card().apply {
            addView(label("Contact information", 16, true))
            addView(label("${attendee.name}\n${attendee.email}\n${attendee.phone}"))
            addView(label("Registration details", 16, true))
            addView(label("Registration status: ${attendee.registrationStatus}"))
            addView(label("QR credential status: ${attendee.qrCredentialStatus}"))
            addView(label("Current event status: ${attendee.currentEventStatus}"))
            addView(label("Event-specific points: ${attendee.points}"))
            addView(label("Recent transaction history", 16, true))
            addView(label(attendee.recentTransactions.ifEmpty { listOf("No recent transactions.") }.joinToString("\n")))
        })
    }
}

open class TransactionLogsActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var selector: Spinner
    private lateinit var filter: Spinner
    private lateinit var logList: LinearLayout
    private lateinit var details: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        val events = repository.getApprovedOrganizerEvents()
        val content = mvpPage(
            title = "Transaction Logs",
            subtitle = "Scan transactions and rejected scans for the selected organizer event.",
        )
        selector = eventSelector(events) { render() }
        filter = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@TransactionLogsActivity,
                android.R.layout.simple_spinner_item,
                listOf("All", "Entry", "Attendance", "Benefit Claim", "Booth/Session Visit", "Reward Redemption", "Exit", "Rejected/Invalid"),
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        logList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(label("Event selector", 15, true))
        content.addView(selector)
        content.addView(filter)
        content.addView(section("Logs"))
        content.addView(logList)
        content.addView(section("Transaction Detail"))
        content.addView(details)
        filter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = render()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        render()
    }

    private fun render() {
        if (!::selector.isInitialized || !::logList.isInitialized) return
        val event = repository.getApprovedOrganizerEvents()[selector.selectedItemPosition.coerceAtLeast(0)]
        val selectedFilter = filter.selectedItem?.toString().orEmpty()
        val logs = repository.getOrganizerTransactions(event.id).filter {
            selectedFilter == "All" ||
                it.type == selectedFilter ||
                (selectedFilter == "Rejected/Invalid" && it.status == "Rejected")
        }
        logList.removeAllViews()
        if (logs.isEmpty()) {
            logList.addView(label("Empty state: there are no logs for this event/filter yet."))
            details.removeAllViews()
            return
        }
        logs.forEach { log ->
            logList.addView(card().apply {
                addView(label("${log.type} - ${log.status}", 17, true))
                addView(label("Attendee: ${log.attendeeName}"))
                addView(label("Timestamp: ${log.timestamp}"))
                addView(label("Staff: ${log.staffName} (${log.staffId})"))
                if (log.status == "Rejected") addView(label("Rejection reason: ${log.reason}", color = "#B3261E"))
                addView(primaryButton("View Transaction") { renderDetails(log) })
            })
        }
        renderDetails(logs.first())
    }

    private fun renderDetails(log: OrganizerMvpTransaction) {
        details.removeAllViews()
        details.addView(card().apply {
            addView(label("Transaction ID: ${log.id}", 16, true))
            addView(label("Event: ${log.eventTitle} (${log.eventId})"))
            addView(label("Attendee: ${log.attendeeName} (${log.attendeeId})"))
            addView(label("Staff: ${log.staffName} (${log.staffId})"))
            addView(label("Scan purpose: ${log.scanPurpose}"))
            addView(label("Result status: ${log.status}"))
            addView(label("Reason/message: ${log.reason}"))
            addView(label("Created timestamp: ${log.timestamp}"))
        })
    }
}

open class EventReportsActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var selector: Spinner
    private lateinit var reportType: Spinner
    private lateinit var reportContent: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        val events = repository.getApprovedOrganizerEvents()
        val content = mvpPage(
            title = "Event Reports",
            subtitle = "Summaries, attendance analytics, transaction breakdowns, rejected scans, rewards, and recent activity.",
        )
        selector = eventSelector(events) { render() }
        reportType = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@EventReportsActivity,
                android.R.layout.simple_spinner_item,
                listOf("Full report", "Attendance", "Transactions", "Rewards"),
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        reportContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(label("Event selector", 15, true))
        content.addView(selector)
        content.addView(EditText(this).apply {
            hint = "Date range placeholder, e.g. 2026-05-26 to 2026-05-30"
        })
        content.addView(reportType)
        content.addView(primaryButton("Generate Report") {
            // TODO: Connect to backend report generation endpoint.
            Toast.makeText(this, "Report generation placeholder", Toast.LENGTH_SHORT).show()
            render()
        })
        content.addView(primaryButton("Export / Download Report") {
            // TODO: Connect to backend export/download implementation.
            Toast.makeText(this, "Export placeholder: backend integration pending", Toast.LENGTH_SHORT).show()
        })
        content.addView(reportContent)
        reportType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = render()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        render()
    }

    private fun render() {
        if (!::selector.isInitialized || !::reportContent.isInitialized) return
        val event = repository.getApprovedOrganizerEvents()[selector.selectedItemPosition.coerceAtLeast(0)]
        val logs = repository.getOrganizerTransactions(event.id)
        reportContent.removeAllViews()
        reportContent.addView(section("Summary Cards"))
        listOf(
            "Total registered" to event.registeredCount,
            "Total entered/checked-in" to event.enteredCount,
            "Total attended" to event.attendedCount,
            "No-shows" to event.noShowCount,
            "Total benefit claims" to event.benefitClaims,
            "Booth/session visits" to event.boothSessionVisits,
            "Reward redemptions" to event.rewardRedemptions,
            "Rejected scans" to event.rejectedScans,
            "Total points awarded" to event.totalPointsAwarded,
        ).forEach { reportContent.addView(statCard(it.first, it.second.toString())) }

        reportContent.addView(section("Attendance Summary"))
        reportContent.addView(label("Registered ${event.registeredCount}, entered ${event.enteredCount}, attended ${event.attendedCount}, exited ${event.exitedCount}, no-shows ${event.noShowCount}."))
        reportContent.addView(section("Transaction Summary by Type"))
        reportContent.addView(label(logs.groupingBy { it.type }.eachCount().ifEmpty { mapOf("No transactions" to 0) }.entries.joinToString("\n") { "${it.key}: ${it.value}" }))
        reportContent.addView(section("Rejected Transaction Summary"))
        reportContent.addView(label(logs.filter { it.status == "Rejected" }.ifEmpty { emptyList() }.joinToString("\n") { "${it.timestamp}: ${it.reason}" }.ifBlank { "No rejected scans." }))
        reportContent.addView(section("Points/Rewards Summary"))
        reportContent.addView(label("Points awarded: ${event.totalPointsAwarded}\nReward redemptions: ${event.rewardRedemptions}\nRewards status: ${event.rewardsStatus}"))
        reportContent.addView(section("Recent Activity"))
        reportContent.addView(label(logs.take(5).joinToString("\n") { "${it.timestamp}: ${it.type} for ${it.attendeeName} (${it.status})" }.ifBlank { "No recent activity." }))
    }
}

open class ManageUsersActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var selector: Spinner
    private lateinit var searchInput: EditText
    private lateinit var results: LinearLayout
    private lateinit var assigned: LinearLayout
    private val assignedStaff = OrganizerMvpPlaceholders.staff.toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        val events = repository.getApprovedOrganizerEvents()
        val content = mvpPage(
            title = "Manage Staff Access",
            subtitle = "Assign or remove staff access for an approved event. Organizer-only access is preserved by navigation role routing.",
        )
        selector = eventSelector(events) { renderAssigned() }
        searchInput = EditText(this).apply { hint = "Search user by name or email" }
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        assigned = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(label("Event selector", 15, true))
        content.addView(selector)
        content.addView(searchInput)
        content.addView(primaryButton("Search Users") { renderSearch() })
        content.addView(stateBox(
            "User not found: shown when search has no matches.",
            "Duplicate staff assignment: add action is blocked.",
            "Unauthorized organizer: TODO backend authorization check.",
            "Save/update failure: TODO backend error handling.",
        ))
        content.addView(section("Available/Search Result Users"))
        content.addView(results)
        content.addView(section("Currently Assigned Staff"))
        content.addView(assigned)
        searchInput.afterTextChanged { renderSearch() }
        renderSearch()
        renderAssigned()
    }

    private fun selectedEvent(): OrganizerMvpEvent =
        repository.getApprovedOrganizerEvents()[selector.selectedItemPosition.coerceAtLeast(0)]

    private fun renderSearch() {
        if (!::results.isInitialized) return
        val event = selectedEvent()
        val users = repository.searchAvailableStaffUsers(searchInput.text.toString())
        results.removeAllViews()
        if (users.isEmpty()) {
            results.addView(label("Validation: user not found."))
            return
        }
        users.forEach { user ->
            results.addView(staffCard(user.copy(assignedEventId = event.id, assignedEvent = event.title), showAdd = true))
        }
    }

    private fun renderAssigned() {
        if (!::assigned.isInitialized) return
        val event = selectedEvent()
        val staffForEvent = assignedStaff.filter { it.assignedEventId == event.id }
        assigned.removeAllViews()
        if (staffForEvent.isEmpty()) {
            assigned.addView(label("Empty state: no staff assigned to ${event.title}."))
            return
        }
        staffForEvent.forEach { assigned.addView(staffCard(it, showAdd = false)) }
    }

    private fun staffCard(staff: OrganizerMvpStaff, showAdd: Boolean): LinearLayout =
        card().apply {
            addView(label(staff.name, 17, true))
            addView(label(staff.email))
            addView(label("Assigned event: ${staff.assignedEvent}"))
            addView(label("Access status: ${staff.accessStatus}"))
            addView(label("Allowed actions/permissions: ${staff.permissions.ifEmpty { listOf("Entry scans", "Attendance scans") }.joinToString(", ")}"))
            if (showAdd) {
                addView(primaryButton("Add staff to event") {
                    if (assignedStaff.any { it.email == staff.email && it.assignedEventId == staff.assignedEventId }) {
                        Toast.makeText(this@ManageUsersActivity, "Duplicate staff assignment", Toast.LENGTH_SHORT).show()
                    } else {
                        assignedStaff.add(staff.copy(accessStatus = "Enabled"))
                        Toast.makeText(this@ManageUsersActivity, "Staff added locally", Toast.LENGTH_SHORT).show()
                        renderAssigned()
                    }
                })
            } else {
                addView(primaryButton("Enable/disable staff access") {
                    val index = assignedStaff.indexOfFirst { it.id == staff.id && it.assignedEventId == staff.assignedEventId }
                    if (index >= 0) {
                        val newStatus = if (assignedStaff[index].accessStatus == "Enabled") "Disabled" else "Enabled"
                        assignedStaff[index] = assignedStaff[index].copy(accessStatus = newStatus)
                        renderAssigned()
                    }
                })
                addView(primaryButton("View staff details") {
                    AlertDialog.Builder(this@ManageUsersActivity)
                        .setTitle(staff.name)
                        .setMessage("Email: ${staff.email}\nEvent: ${staff.assignedEvent}\nStatus: ${staff.accessStatus}\nPermissions: ${staff.permissions.joinToString(", ")}")
                        .setPositiveButton("Close", null)
                        .show()
                })
                addView(primaryButton("Remove staff from event") {
                    AlertDialog.Builder(this@ManageUsersActivity)
                        .setTitle("Remove staff?")
                        .setMessage("Remove ${staff.name} from ${staff.assignedEvent}?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove") { _, _ ->
                            assignedStaff.removeAll { it.id == staff.id && it.assignedEventId == staff.assignedEventId }
                            renderAssigned()
                        }
                        .show()
                })
            }
        }
}

open class ManageScanPurposesActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var content: LinearLayout
    private val purposeViews = mutableListOf<Pair<OrganizerMvpScanPurpose, LinearLayout>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        content = mvpPage(
            title = "Scan Purpose Management",
            subtitle = "Configure scan purposes and transaction rules for staff scanning.",
        )
        content.addView(label("Event selector", 15, true))
        content.addView(eventSelector(repository.getApprovedOrganizerEvents()) { })
        content.addView(section("Scan Purposes"))
        repository.getOrganizerScanPurposes().forEach { purpose ->
            val purposeCard = scanPurposeCard(purpose)
            purposeViews.add(purpose to purposeCard)
            content.addView(purposeCard)
        }
        content.addView(section("Transaction Rules"))
        listOf(
            "Prevent duplicate entry",
            "Prevent duplicate attendance if configured",
            "Prevent duplicate benefit claim",
            "Prevent duplicate reward claim",
            "Reject wrong event QR",
            "Reject inactive/invalid registration",
            "Reject unauthorized staff scan",
        ).forEach { content.addView(CheckBox(this).apply { text = it; isChecked = true }) }
        content.addView(stateBox(
            "Validation: conflicting rules are blocked before save.",
            "Validation: invalid point values must be zero or greater.",
        ))
        content.addView(primaryButton("Add/Edit scan purpose rule") {
            Toast.makeText(this, "Edit placeholder: fields are editable above", Toast.LENGTH_SHORT).show()
        })
        content.addView(primaryButton("Save configuration") { validateAndSave() })
        content.addView(primaryButton("Reset / Cancel changes") {
            Toast.makeText(this, "Changes reset placeholder", Toast.LENGTH_SHORT).show()
            recreate()
        })
    }

    private fun scanPurposeCard(purpose: OrganizerMvpScanPurpose): LinearLayout =
        card().apply {
            addView(label(purpose.label, 17, true))
            addView(CheckBox(this@ManageScanPurposesActivity).apply { text = "Enabled"; isChecked = purpose.enabled })
            addView(EditText(this@ManageScanPurposesActivity).apply {
                hint = "Duplicate rule setting"
                setText(purpose.duplicateRule)
            })
            addView(CheckBox(this@ManageScanPurposesActivity).apply { text = "Tracking-only"; isChecked = purpose.trackingOnly })
            addView(CheckBox(this@ManageScanPurposesActivity).apply { text = "Points enabled"; isChecked = purpose.pointsEnabled })
            addView(EditText(this@ManageScanPurposesActivity).apply {
                hint = "Points value"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                setText(purpose.pointsValue.toString())
            })
            addView(EditText(this@ManageScanPurposesActivity).apply {
                hint = "Required selection label"
                setText(purpose.requiredSelectionLabel)
            })
        }

    private fun validateAndSave() {
        val errors = mutableListOf<String>()
        purposeViews.forEach { (purpose, view) ->
            val trackingOnly = (view.getChildAt(3) as CheckBox).isChecked
            val pointsEnabled = (view.getChildAt(4) as CheckBox).isChecked
            val pointsValue = (view.getChildAt(5) as EditText).text.toString().toIntOrNull()
            if (pointsValue == null || pointsValue < 0) errors.add("${purpose.label}: invalid point value")
            if (trackingOnly && pointsEnabled) errors.add("${purpose.label}: tracking-only cannot award points in this MVP rule set")
        }
        if (errors.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Validation errors")
                .setMessage(errors.joinToString("\n"))
                .setPositiveButton("OK", null)
                .show()
        } else {
            // TODO: Persist scan purpose configuration to backend.
            Toast.makeText(this, "Configuration saved locally", Toast.LENGTH_SHORT).show()
        }
    }
}

open class AttendeeDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val attendeeId = intent.getStringExtra("extra_attendee_id").orEmpty()
        val attendee = OrganizerMvpPlaceholders.attendees.firstOrNull { it.id == attendeeId }
        val content = mvpPage(
            title = "Attendee Details",
            subtitle = "Standalone attendee detail placeholder.",
        )
        content.gravity = Gravity.NO_GRAVITY
        content.addView(label(attendee?.let {
            "${it.name}\n${it.email}\n${it.phone}\nStatus: ${it.currentEventStatus}\nPoints: ${it.points}"
        } ?: "Attendee detail placeholder. Select a row from Attendee Management to pass an attendee ID."))
    }
}

open class ReportsActivity : EventReportsActivity()

open class ManageRewardsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = mvpPage(
            title = "Reward Management",
            subtitle = "Existing reward management placeholder. Organizer reports and event details show reward status for this MVP.",
        )
        content.addView(label(OrganizerMvpPlaceholders.TODO_BACKEND))
    }
}

open class NotificationManagementActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = mvpPage(
            title = "Notification Management",
            subtitle = "Existing notification management placeholder retained for organizer navigation safety.",
        )
        content.addView(label(OrganizerMvpPlaceholders.TODO_BACKEND))
    }
}
