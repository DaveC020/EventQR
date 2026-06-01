package com.thedavelopers.eventqr.features.organizer.attendees

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpAttendee
import com.thedavelopers.eventqr.features.organizer.OrganizerMvpEvent
import com.thedavelopers.eventqr.features.organizer.OrganizerRepository
import com.thedavelopers.eventqr.features.organizer.attendeeInitial
import com.thedavelopers.eventqr.features.organizer.intentEventId
import com.thedavelopers.eventqr.features.organizer.resolveSelectedEvent
import com.thedavelopers.eventqr.features.organizer.selectedEventId
import com.thedavelopers.eventqr.features.organizer.statusBucket
import com.thedavelopers.eventqr.features.organizer.statusPalette
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

open class AttendeeDetailsActivity : AppCompatActivity() {
    private lateinit var repository: OrganizerRepository
    private lateinit var selectedEvent: OrganizerMvpEvent
    private lateinit var attendee: OrganizerMvpAttendee
    private val displayZone: ZoneId = ZoneId.of("Asia/Manila")
    private val displayTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendee_details)
        repository = OrganizerRepository(this)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val attendeeId = intent.getStringExtra(SearchAttendeesActivity.EXTRA_ATTENDEE_ID).orEmpty()
        val eventId = intentEventId().orEmpty().ifBlank { selectedEventId() }
        if (attendeeId.isBlank() || eventId.isBlank()) {
            Toast.makeText(this, "Open attendee details from an event to view live records.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        selectedEvent = resolveSelectedEvent(repository.getApprovedOrganizerEvents(), eventId)
            ?: run {
                Toast.makeText(this, "Selected event not available.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

        findViewById<TextView>(R.id.txtDetailTitle).text = "Attendee Details"
        findViewById<View>(R.id.btnPrintId).setOnClickListener {
            Toast.makeText(this, "Print ID flow is not wired on this screen.", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btnScanAgain).setOnClickListener {
            Toast.makeText(this, "Scan Again flow is not wired on this screen.", Toast.LENGTH_SHORT).show()
        }

        MainScope().launch {
            val attendeeLoad = repository.loadAttendeesForMvp(eventId)
            attendee = attendeeLoad.data.firstOrNull { it.id == attendeeId } ?: run {
                Toast.makeText(this@AttendeeDetailsActivity, "Attendee record not found for this event.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            renderProfile()
            renderDetails()
        }
    }

    private fun renderProfile() {
        findViewById<TextView>(R.id.txtDetailInitial).text = attendeeInitial(attendee.name)
        findViewById<TextView>(R.id.txtDetailName).text = attendee.name
        findViewById<TextView>(R.id.txtDetailEmail).text = attendee.email
        findViewById<TextView>(R.id.txtDetailPhone).text = attendee.phone
    }

    private fun renderDetails() {
        findViewById<TextView>(R.id.txtDetailEventValue).text = selectedEvent.title
        findViewById<TextView>(R.id.txtDetailRegistrationIdValue).text = formatRegistrationId(attendee.id)
        findViewById<TextView>(R.id.txtDetailStatusValue).apply {
            text = attendee.statusBucket()
            val (bgColor, textColor) = attendee.statusPalette()
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 999f
            }
            setTextColor(textColor)
        }
        findViewById<TextView>(R.id.txtDetailCheckInTimeValue).text = formatCheckInTime(attendee.lastTransactionTime)
        findViewById<TextView>(R.id.txtDetailPointsValue).text = "${attendee.points} pts"
        findViewById<TextView>(R.id.txtDetailTransactionsValue).text = attendee.recentTransactions.size.toString()
    }

    private fun formatRegistrationId(value: String): String {
        val cleaned = value.trim()
        if (cleaned.isBlank()) return "-"
        if (cleaned.length <= 12) return cleaned
        return "REG-${cleaned.take(8).uppercase(Locale.ENGLISH)}"
    }

    private fun formatCheckInTime(value: String): String {
        val cleaned = value.trim()
        if (cleaned.isBlank()) return "-"
        return parseToLocalDateTime(cleaned)?.format(displayTimeFormatter) ?: cleaned
    }

    private fun parseToLocalDateTime(value: String): LocalDateTime? {
        val normalized = value.replace(" ", "T").trim()
        return runCatching { Instant.parse(normalized).atZone(displayZone).toLocalDateTime() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(normalized).atZoneSameInstant(displayZone).toLocalDateTime() }.getOrNull()
            ?: runCatching { ZonedDateTime.parse(normalized).withZoneSameInstant(displayZone).toLocalDateTime() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }.getOrNull()
            ?: runCatching { LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)) }.getOrNull()
            ?: runCatching { LocalDateTime.parse(value, DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH)) }.getOrNull()
            ?: runCatching { LocalDateTime.parse(value, DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a", Locale.ENGLISH)) }.getOrNull()
    }
}
