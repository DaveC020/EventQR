package com.thedavelopers.eventqr.features.attendee

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.session.SessionManager
import com.thedavelopers.eventqr.core.util.Validators
import com.thedavelopers.eventqr.features.events.model.dto.EventCreationRequestDto
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RequestEventActivity : AppCompatActivity() {
    private lateinit var repository: AttendeeRepository
    private lateinit var sessionManager: SessionManager

    private lateinit var eventNameInput: EditText
    private lateinit var eventDescriptionInput: EditText
    private lateinit var eventCategoryInput: EditText
    private lateinit var targetAudienceInput: EditText
    private lateinit var capacityInput: EditText
    private lateinit var venueInput: EditText
    private lateinit var startDateTimeInput: EditText
    private lateinit var endDateTimeInput: EditText
    private lateinit var registrationStartDateTimeInput: EditText
    private lateinit var registrationEndDateTimeInput: EditText
    private lateinit var requesterNameInput: EditText
    private lateinit var contactEmailInput: EditText
    private lateinit var contactNumberInput: EditText
    private lateinit var eventPosterPreview: ImageView
    private lateinit var eventPosterPlaceholder: View
    private lateinit var eventPosterStatusText: TextView
    private lateinit var reasonForRequestInput: EditText
    private lateinit var formMessageText: TextView
    private lateinit var submitProgress: ProgressBar
    private lateinit var submitButton: Button
    private var successDialog: AlertDialog? = null
    private var isSubmitting = false
    private var selectedPosterFile: File? = null

    private val displayDateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
    private val zoneId: ZoneId = ZoneId.of("Asia/Manila")

    private var startDateTimeValue: LocalDateTime? = null
    private var endDateTimeValue: LocalDateTime? = null
    private var registrationStartDateTimeValue: LocalDateTime? = null
    private var registrationEndDateTimeValue: LocalDateTime? = null

    private val posterPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handlePosterSelected(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_event)

        repository = AttendeeRepository(this)
        sessionManager = SessionManager(this)
        bindViews()
        prefillRequester()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.backText).setOnClickListener { finish() }
        findViewById<Button>(R.id.cancelButton).setOnClickListener { finish() }
        findViewById<View>(R.id.eventPosterPicker).setOnClickListener { posterPicker.launch("image/*") }
        submitButton.setOnClickListener { submitRequest() }

        configureDateTimeField(startDateTimeInput, { startDateTimeValue }) { value ->
            startDateTimeValue = value
            startDateTimeInput.setText(formatForDisplay(value))
            startDateTimeInput.error = null
            if (endDateTimeValue != null && !endDateTimeValue!!.isAfter(value)) {
                endDateTimeValue = null
                endDateTimeInput.text?.clear()
                endDateTimeInput.error = "End date/time must be after the start"
            }
            if (registrationEndDateTimeValue != null && !registrationEndDateTimeValue!!.isBefore(value)) {
                registrationEndDateTimeValue = null
                registrationEndDateTimeInput.text?.clear()
                registrationEndDateTimeInput.error = "Registration end must be before event start"
            }
        }
        configureDateTimeField(endDateTimeInput, { endDateTimeValue }) { value ->
            endDateTimeValue = value
            endDateTimeInput.setText(formatForDisplay(value))
            endDateTimeInput.error = null
            if (startDateTimeValue != null && !value.isAfter(startDateTimeValue)) {
                endDateTimeValue = null
                endDateTimeInput.text?.clear()
                endDateTimeInput.error = "End date/time must be after the start"
            }
        }
        configureDateTimeField(registrationStartDateTimeInput, { registrationStartDateTimeValue }) { value ->
            registrationStartDateTimeValue = value
            registrationStartDateTimeInput.setText(formatForDisplay(value))
            registrationStartDateTimeInput.error = null
            if (registrationEndDateTimeValue != null && !registrationEndDateTimeValue!!.isAfter(value)) {
                registrationEndDateTimeValue = null
                registrationEndDateTimeInput.text?.clear()
                registrationEndDateTimeInput.error = "Registration end must be after registration start"
            }
        }
        configureDateTimeField(registrationEndDateTimeInput, { registrationEndDateTimeValue }) { value ->
            registrationEndDateTimeValue = value
            registrationEndDateTimeInput.setText(formatForDisplay(value))
            registrationEndDateTimeInput.error = null
            if (registrationStartDateTimeValue != null && !value.isAfter(registrationStartDateTimeValue)) {
                registrationEndDateTimeValue = null
                registrationEndDateTimeInput.text?.clear()
                registrationEndDateTimeInput.error = "Registration end must be after registration start"
                return@configureDateTimeField
            }
            if (startDateTimeValue != null && !value.isBefore(startDateTimeValue)) {
                registrationEndDateTimeValue = null
                registrationEndDateTimeInput.text?.clear()
                registrationEndDateTimeInput.error = "Registration end must be before event start"
            }
        }
    }

    private fun bindViews() {
        eventNameInput = findViewById(R.id.eventNameInput)
        eventDescriptionInput = findViewById(R.id.eventDescriptionInput)
        eventCategoryInput = findViewById(R.id.eventCategoryInput)
        targetAudienceInput = findViewById(R.id.targetAudienceInput)
        capacityInput = findViewById(R.id.capacityInput)
        venueInput = findViewById(R.id.venueInput)
        startDateTimeInput = findViewById(R.id.startDateTimeInput)
        endDateTimeInput = findViewById(R.id.endDateTimeInput)
        registrationStartDateTimeInput = findViewById(R.id.registrationStartDateTimeInput)
        registrationEndDateTimeInput = findViewById(R.id.registrationEndDateTimeInput)
        requesterNameInput = findViewById(R.id.requesterNameInput)
        contactEmailInput = findViewById(R.id.contactEmailInput)
        contactNumberInput = findViewById(R.id.contactNumberInput)
        eventPosterPreview = findViewById(R.id.eventPosterPreview)
        eventPosterPlaceholder = findViewById(R.id.eventPosterPlaceholder)
        eventPosterStatusText = findViewById(R.id.eventPosterStatusText)
        reasonForRequestInput = findViewById(R.id.reasonForRequestInput)
        formMessageText = findViewById(R.id.formMessageText)
        submitProgress = findViewById(R.id.submitProgress)
        submitButton = findViewById(R.id.submitRequestButton)
    }

    private fun prefillRequester() {
        requesterNameInput.setText(sessionManager.getFullName().orEmpty())
        contactEmailInput.setText(sessionManager.getEmail().orEmpty())

        lifecycleScope.launch {
            when (val result = repository.getMyProfile()) {
                is NetworkResult.Success -> {
                    if (requesterNameInput.textString().isBlank()) requesterNameInput.setText(result.data.fullName)
                    if (contactEmailInput.textString().isBlank()) contactEmailInput.setText(result.data.email)
                    if (contactNumberInput.textString().isBlank()) contactNumberInput.setText(result.data.phoneNumber.orEmpty())
                }
                is NetworkResult.Error -> Unit
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun handlePosterSelected(uri: Uri) {
        hideMessage()
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        val width = options.outWidth
        val height = options.outHeight
        val ratio = if (height == 0) 0.0 else width.toDouble() / height.toDouble()

        if (width < 1200 || height < 675 || ratio < 1.55 || ratio > 1.90) {
            showMessage("Event poster must be at least 1200 x 675 pixels and use a landscape 16:9-style ratio.")
            return
        }

        val extension = contentResolver.getType(uri)?.substringAfterLast('/')?.lowercase()?.takeIf { it.length <= 5 } ?: "jpg"
        val posterFile = File(cacheDir, "event_poster_${System.currentTimeMillis()}.$extension")
        runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                posterFile.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Unable to open selected image")
        }.onSuccess {
            selectedPosterFile = posterFile
            eventPosterPreview.setImageURI(uri)
            eventPosterPreview.visibility = View.VISIBLE
            eventPosterPlaceholder.visibility = View.GONE
            eventPosterStatusText.text = "Poster selected. It will appear on the Event Details header after approval."
            eventPosterStatusText.setTextColor(0xFF4F46E5.toInt())
        }.onFailure {
            showMessage("Unable to attach selected poster. Please choose another image.")
        }
    }

    private fun submitRequest() {
        if (isSubmitting) return
        hideMessage()
        buildValidatedRequest(eventPosterFileId = null) ?: return
        setLoading(true)

        lifecycleScope.launch {
            val posterFileId = selectedPosterFile?.let { file ->
                when (val uploadResult = repository.uploadEventPoster(file)) {
                    is NetworkResult.Success -> uploadResult.data.fileId.toString()
                    is NetworkResult.Error -> {
                        setLoading(false)
                        showMessage(uploadResult.message.ifBlank { "Could not upload event poster. Please try another image." })
                        return@launch
                    }
                    NetworkResult.Loading -> null
                }
            }

            val request = buildValidatedRequest(eventPosterFileId = posterFileId) ?: run {
                setLoading(false)
                return@launch
            }

            when (val result = repository.createEventRequest(request)) {
                is NetworkResult.Success -> {
                    setLoading(false)
                    showSuccessDialog()
                }
                is NetworkResult.Error -> {
                    setLoading(false)
                    showMessage(result.message.ifBlank { "Could not submit request. Please try again." })
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    private fun buildValidatedRequest(eventPosterFileId: String?): EventCreationRequestDto? {
        clearFieldErrors()

        val eventName = eventNameInput.required("Event name is required") ?: return null
        val eventDescription = eventDescriptionInput.required("Event description is required") ?: return null
        val eventCategory = eventCategoryInput.required("Event category is required") ?: return null
        val targetAudience = targetAudienceInput.required("Target audience is required") ?: return null
        val venue = venueInput.required("Venue/location is required") ?: return null
        val startDateTime = startDateTimeValue ?: run {
            startDateTimeInput.error = "Start date/time is required"
            return null
        }
        val endDateTime = endDateTimeValue ?: run {
            endDateTimeInput.error = "End date/time is required"
            return null
        }
        val capacity = capacityInput.positiveInt("Capacity must be a positive number") ?: return null
        val requesterName = requesterNameInput.required("Requester name is required") ?: return null
        val contactEmail = contactEmailInput.required("Contact email is required") ?: return null
        val contactNumber = contactNumberInput.required("Contact number is required") ?: return null
        val reason = reasonForRequestInput.required("Reason for request is required") ?: return null

        if (!Validators.isValidEmail(contactEmail)) {
            contactEmailInput.error = "Enter a valid email address"
            contactEmailInput.requestFocus()
            return null
        }
        if (startDateTime.isBefore(currentLocalDateTime())) {
            startDateTimeInput.error = "Start date/time cannot be in the past"
            return null
        }
        if (!endDateTime.isAfter(startDateTime)) {
            endDateTimeInput.error = "End date/time must be after the start"
            return null
        }

        val registrationStart = registrationStartDateTimeValue ?: run {
            registrationStartDateTimeInput.error = "Registration start date/time is required"
            return null
        }
        val registrationEnd = registrationEndDateTimeValue ?: run {
            registrationEndDateTimeInput.error = "Registration end date/time is required"
            return null
        }

        if (registrationStart.isBefore(currentLocalDateTime())) {
            registrationStartDateTimeInput.error = "Registration start cannot be in the past"
            return null
        }
        if (!registrationEnd.isAfter(registrationStart)) {
            registrationEndDateTimeInput.error = "Registration end must be after registration start"
            return null
        }
        if (registrationEnd.isBefore(currentLocalDateTime())) {
            registrationEndDateTimeInput.error = "Registration end cannot be in the past"
            return null
        }
        if (registrationEnd.isAfter(endDateTime)) {
            registrationEndDateTimeInput.error = "Registration end must not be after event end"
            return null
        }
        if (!registrationEnd.isBefore(startDateTime)) {
            registrationEndDateTimeInput.error = "Registration end must be before event start"
            return null
        }

        return EventCreationRequestDto(
            eventName = eventName,
            eventDescription = eventDescription,
            eventCategory = eventCategory,
            targetAudience = targetAudience,
            capacity = capacity,
            venue = venue,
            startDateTime = startDateTime.atZone(zoneId).toInstant().toString(),
            endDateTime = endDateTime.atZone(zoneId).toInstant().toString(),
            registrationStartDateTime = registrationStart.atZone(zoneId).toInstant().toString(),
            registrationEndDateTime = registrationEnd.atZone(zoneId).toInstant().toString(),
            requesterName = requesterName,
            contactEmail = contactEmail,
            contactNumber = contactNumber,
            requestedFeatures = null,
            eventLogoUrl = eventPosterFileId,
            additionalNotes = null,
            reasonForRequest = reason,
        )
    }

    private fun setLoading(loading: Boolean) {
        isSubmitting = loading
        submitProgress.visibility = if (loading) View.VISIBLE else View.GONE
        submitButton.isEnabled = !loading
        submitButton.text = if (loading) "Submitting..." else "Submit Request"
    }

    private fun showSuccessDialog() {
        if (isFinishing || isDestroyed) return
        successDialog?.dismiss()

        val dialogView = layoutInflater.inflate(R.layout.dialog_event_request_submitted, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<Button>(R.id.successViewRequestsButton).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, MyEventRequestsActivity::class.java))
            finish()
        }
        dialogView.findViewById<Button>(R.id.successDashboardButton).setOnClickListener {
            dialog.dismiss()
            startActivity(
                Intent(this, com.thedavelopers.eventqr.features.dashboard.DashboardActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
            finish()
        }

        successDialog = dialog
        dialog.show()
    }

    override fun onDestroy() {
        successDialog?.dismiss()
        successDialog = null
        super.onDestroy()
    }

    private fun showMessage(message: String) {
        formMessageText.text = message
        formMessageText.visibility = View.VISIBLE
    }

    private fun hideMessage() {
        formMessageText.text = ""
        formMessageText.visibility = View.GONE
    }

    private fun clearFieldErrors() {
        listOf(
            eventNameInput, eventDescriptionInput, eventCategoryInput, targetAudienceInput,
            capacityInput, venueInput, startDateTimeInput, endDateTimeInput,
            registrationStartDateTimeInput, registrationEndDateTimeInput,
            requesterNameInput, contactEmailInput, contactNumberInput,
            reasonForRequestInput,
        ).forEach { it.error = null }
    }

    private fun configureDateTimeField(
        field: EditText,
        getCurrentValue: () -> LocalDateTime?,
        onSelected: (LocalDateTime) -> Unit,
    ) {
        field.isFocusable = false
        field.isFocusableInTouchMode = false
        field.isCursorVisible = false
        field.isLongClickable = false
        field.setTextIsSelectable(false)
        field.setOnClickListener {
            showDateTimePicker(
                initialValue = getCurrentValue() ?: currentLocalDateTime(),
                onSelected = onSelected,
            )
        }
    }

    private fun showDateTimePicker(initialValue: LocalDateTime, onSelected: (LocalDateTime) -> Unit) {
        val now = currentLocalDateTime()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                val initialTime = if (selectedDate == initialValue.toLocalDate()) {
                    initialValue.toLocalTime().withSecond(0).withNano(0)
                } else if (selectedDate == now.toLocalDate()) {
                    now.toLocalTime().withSecond(0).withNano(0)
                } else {
                    LocalTime.of(9, 0)
                }

                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        val selectedDateTime = LocalDateTime.of(selectedDate, LocalTime.of(hourOfDay, minute))
                        if (selectedDateTime.isBefore(currentLocalDateTime())) {
                            Toast.makeText(this, "Selected date/time cannot be in the past", Toast.LENGTH_SHORT).show()
                            return@TimePickerDialog
                        }
                        onSelected(selectedDateTime)
                    },
                    initialTime.hour,
                    initialTime.minute,
                    false,
                ).show()
            },
            initialValue.year,
            initialValue.monthValue - 1,
            initialValue.dayOfMonth,
        ).apply {
            datePicker.minDate = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
        }.show()
    }

    private fun formatForDisplay(value: LocalDateTime): String = value.format(displayDateTimeFormatter)
    private fun currentLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.now(), zoneId)

    private fun EditText.required(errorMessage: String): String? {
        val value = textString()
        if (value.isBlank()) {
            error = errorMessage
            requestFocus()
            return null
        }
        return value
    }

    private fun EditText.positiveInt(errorMessage: String): Int? {
        val value = textString().toIntOrNull()
        if (value == null || value <= 0) {
            error = errorMessage
            requestFocus()
            return null
        }
        return value
    }

    private fun EditText.textString(): String = text?.toString()?.trim().orEmpty()
}
