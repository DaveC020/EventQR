package com.thedavelopers.eventqr.features.staff.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.dto.AccountRole
import com.thedavelopers.eventqr.core.session.SessionManager
import com.thedavelopers.eventqr.core.util.RoleMapper
import com.thedavelopers.eventqr.features.staff.EventSpinnerOption
import com.thedavelopers.eventqr.features.staff.StaffBottomNavItem
import com.thedavelopers.eventqr.features.staff.StaffDashboardActivity
import com.thedavelopers.eventqr.features.staff.StaffProfileActivity
import com.thedavelopers.eventqr.features.staff.StaffRepository
import com.thedavelopers.eventqr.features.staff.StaffTransactionsActivity
import com.thedavelopers.eventqr.features.staff.StaffScreenExtras
import com.thedavelopers.eventqr.features.staff.StaffCameraScannerActivity
import com.thedavelopers.eventqr.features.staff.configureStaffBottomNav
import com.thedavelopers.eventqr.features.staff.model.dto.ScanVerificationResponse
import com.thedavelopers.eventqr.features.staff.result.StaffScanResultActivity
import com.thedavelopers.eventqr.features.transactions.TransactionAdapter
import com.thedavelopers.eventqr.features.transactions.model.dto.TransactionResponse
import com.thedavelopers.eventqr.features.scanpurposes.model.dto.ScanPurposeResponse
import org.json.JSONObject

open class ScannerActivity : AppCompatActivity(), ScannerContract.View {
    private lateinit var presenter: ScannerPresenter
    private lateinit var eventSpinner: Spinner
    private lateinit var purposeSpinner: Spinner
    private lateinit var qrInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var resultText: TextView
    private lateinit var adapter: TransactionAdapter
    private var staffUserId: String? = null

    private val eventOptions = mutableListOf<EventSpinnerOption>()
    private val purposeOptions = mutableListOf<ScanPurposeResponse>()
    private var preselectedEventId: String? = null
    private var lastSubmittedSignature: String? = null
    private var lastSubmittedAtMs: Long = 0L
    private var submitInFlight: Boolean = false

    private val tag = "StaffQrScanner"
    private val duplicateWindowMs = 2_000L

    private val cameraScanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.d(tag, "camera result canceled")
            return@registerForActivityResult
        }
        val scannedValue = result.data?.getStringExtra(StaffScreenExtras.EXTRA_QR_VALUE)?.trim().orEmpty()
        if (scannedValue.isBlank()) {
            showMessage("Camera did not capture a QR value.")
            Log.w(tag, "camera result missing raw QR value")
            return@registerForActivityResult
        }

        Log.d(tag, "raw QR value detected: $scannedValue")
        val parsed = parseQrPayload(scannedValue)
        if (parsed == null || parsed.qrValue.isBlank()) {
            showMessage("QR payload format is invalid.")
            Log.w(tag, "invalid QR payload format raw=$scannedValue")
            return@registerForActivityResult
        }

        Log.d(tag, "parsed QR value=${parsed.qrValue} parsedCredentialId=${parsed.qrCredentialId}")
        qrInput.setText(parsed.qrValue)
        qrInput.setSelection(parsed.qrValue.length)
        submitCurrentSelection(trigger = "camera")
    }
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.d(tag, "camera permission result granted=$granted")
        if (granted) {
            openCameraScanner()
        } else {
            Toast.makeText(this, "Camera permission is required for QR scanning", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preselectedEventId = intent.getStringExtra(StaffScreenExtras.EXTRA_EVENT_ID)

        val sessionManager = SessionManager(this)
        if (RoleMapper.normalizeRole(sessionManager.getUserRole()) != AccountRole.STAFF.name) {
            Toast.makeText(this, "Access Denied: Staff only", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_staff_scanner)

        presenter = ScannerPresenter(this, StaffRepository(this))
        eventSpinner = findViewById(R.id.spnScannerEvent)
        purposeSpinner = findViewById(R.id.spnScannerPurpose)
        qrInput = findViewById(R.id.edtScannerQr)
        notesInput = findViewById(R.id.edtScannerNotes)
        resultText = findViewById(R.id.txtScannerResult)
        adapter = TransactionAdapter()
        staffUserId = SessionManager(this).getUserId()

        configureStaffBottomNav(StaffBottomNavItem.SCAN)

        findViewById<RecyclerView>(R.id.recyclerScannerResults).apply {
            layoutManager = LinearLayoutManager(this@ScannerActivity)
            adapter = this@ScannerActivity.adapter
        }

        findViewById<View>(R.id.layoutScannerPlaceholder)?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCameraScanner()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        findViewById<Button>(R.id.btnSubmitScan).setOnClickListener {
            submitCurrentSelection(trigger = "manual")
        }

        presenter.loadEvents()
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    override fun showEvents(items: List<EventSpinnerOption>) {
        eventOptions.clear()
        eventOptions.addAll(items)
        val labels = items.map { it.label }
        eventSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        eventSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadSelectedPurposes()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        if (!preselectedEventId.isNullOrBlank()) {
            val index = items.indexOfFirst { it.id == preselectedEventId }
            if (index >= 0) eventSpinner.setSelection(index)
        }
        loadSelectedPurposes()
    }

    override fun showPurposes(items: List<ScanPurposeResponse>) {
        val activePurposes = items.filter { it.active }
        val selectedEventId = eventOptions.getOrNull(eventSpinner.selectedItemPosition)?.id
        val labels = activePurposes.map { it.name }
        Log.d(
            tag,
            "eventId=$selectedEventId loadedScanPurposeCount=${items.size} displayedOptionLabels=$labels"
        )
        
        purposeOptions.clear()
        purposeOptions.addAll(activePurposes)
        
        if (activePurposes.isEmpty()) {
            purposeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("No scan purposes enabled for this event."))
            purposeSpinner.isEnabled = false
        } else {
            purposeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, activePurposes.map { it.name })
            purposeSpinner.isEnabled = true
        }
    }

    override fun appendScanResult(result: TransactionResponse) {
        adapter.submitItems(listOf(result))
    }

    override fun showVerificationResult(result: ScanVerificationResponse) {
        submitInFlight = false
        Log.d(
            tag,
            "backend verification result=SUCCESS eventId=${result.eventId} scanPurposeId=${result.scanPurposeId} message=${result.message}"
        )
        resultText.text = result.message
        openVerificationResult(result)
    }

    override fun showScanError(message: String) {
        submitInFlight = false
        Log.w(tag, "backend verification result=ERROR message=$message")
        resultText.text = message
        showMessage(message)
        openRejectedResult(message)
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showLoading(isLoading: Boolean) {
        findViewById<View>(R.id.progressScanner).visibility = if (isLoading) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnSubmitScan).isEnabled = !isLoading
    }

    private fun loadSelectedPurposes() {
        val selectedEvent = eventOptions.getOrNull(eventSpinner.selectedItemPosition)
        if (selectedEvent != null) {
            presenter.loadPurposes(selectedEvent.id)
        }
    }

    private fun openCameraScanner() {
        Log.d(tag, "launching camera scanner")
        cameraScanLauncher.launch(Intent(this, StaffCameraScannerActivity::class.java))
    }

    private fun submitCurrentSelection(trigger: String) {
        val selectedEvent = eventOptions.getOrNull(eventSpinner.selectedItemPosition)
        if (selectedEvent == null) {
            showMessage("No assigned event selected.")
            Log.w(tag, "submit blocked: missing selected event")
            return
        }
        if (purposeOptions.isEmpty()) {
            showMessage("No scan purposes enabled for this event.")
            Log.w(tag, "submit blocked: no enabled scan purposes eventId=${selectedEvent.id}")
            return
        }
        val selectedPurpose = purposeOptions.getOrNull(purposeSpinner.selectedItemPosition)
        if (selectedPurpose == null) {
            showMessage("No scan purpose selected.")
            Log.w(tag, "submit blocked: missing selected scan purpose eventId=${selectedEvent.id}")
            return
        }

        val qrValue = qrInput.text.toString().trim()
        if (qrValue.isBlank()) {
            showMessage("QR payload format is invalid.")
            Log.w(tag, "submit blocked: empty QR value")
            return
        }

        val signature = "${selectedEvent.id}|${selectedPurpose.scanPurposeId}|$qrValue"
        val now = SystemClock.elapsedRealtime()
        if (submitInFlight || (signature == lastSubmittedSignature && now - lastSubmittedAtMs < duplicateWindowMs)) {
            showMessage("Scan is already being processed. Please wait.")
            Log.w(tag, "duplicate rapid submission prevented signature=$signature")
            return
        }

        submitInFlight = true
        lastSubmittedSignature = signature
        lastSubmittedAtMs = now
        Log.d(
            tag,
            "submit trigger=$trigger selectedEventId=${selectedEvent.id} selectedScanPurposeId=${selectedPurpose.scanPurposeId} selectedScanPurposeCode=${selectedPurpose.code} qrValue=$qrValue"
        )
        presenter.submitScan(
            selectedEvent.id,
            selectedPurpose,
            qrValue,
            notesInput.text.toString(),
            staffUserId
        )
    }

    private fun openVerificationResult(result: ScanVerificationResponse) {
        val selectedPurpose = purposeOptions.getOrNull(purposeSpinner.selectedItemPosition)
        val selectedEvent = eventOptions.getOrNull(eventSpinner.selectedItemPosition)
        startActivity(Intent(this, StaffScanResultActivity::class.java).apply {
            putExtra(StaffScreenExtras.EXTRA_IS_VALID, true)
            putExtra(StaffScreenExtras.EXTRA_MESSAGE, result.message.orEmpty())
            putExtra(StaffScreenExtras.EXTRA_EVENT_ID, result.eventId.toString())
            putExtra(StaffScreenExtras.EXTRA_EVENT_TITLE, selectedEvent?.label.orEmpty())
            putExtra(StaffScreenExtras.EXTRA_SCAN_PURPOSE_ID, result.scanPurposeId.toString())
            putExtra(StaffScreenExtras.EXTRA_SCAN_PURPOSE_NAME, selectedPurpose?.name ?: result.scanPurposeCode.name)
            putExtra(StaffScreenExtras.EXTRA_SCAN_PURPOSE_CODE, result.scanPurposeCode.name)
            putExtra(StaffScreenExtras.EXTRA_QR_VALUE, result.qrValue)
            putExtra(StaffScreenExtras.EXTRA_STAFF_USER_ID, staffUserId.orEmpty())
            putExtra(StaffScreenExtras.EXTRA_ATTENDEE_ID, result.attendeeUserId.toString())
            putExtra(StaffScreenExtras.EXTRA_ATTENDEE_NAME, result.attendeeName.orEmpty())
            putExtra(StaffScreenExtras.EXTRA_ATTENDEE_EMAIL, result.attendeeEmail.orEmpty())
            putExtra(StaffScreenExtras.EXTRA_REGISTRATION_ID, result.registrationId.toString())
            putExtra(StaffScreenExtras.EXTRA_QR_CREDENTIAL_ID, result.qrCredentialId.toString())
            putExtra(StaffScreenExtras.EXTRA_REGISTRATION_STATUS, result.registrationStatus.name)
            putExtra(StaffScreenExtras.EXTRA_QR_ACTIVE, result.qrActive)
            putExtra(StaffScreenExtras.EXTRA_VERIFIED_AT, result.verifiedAt?.toString().orEmpty())
        })
    }

    private fun openRejectedResult(message: String) {
        val selectedPurpose = purposeOptions.getOrNull(purposeSpinner.selectedItemPosition)
        val selectedEvent = eventOptions.getOrNull(eventSpinner.selectedItemPosition)
        startActivity(Intent(this, StaffScanResultActivity::class.java).apply {
            putExtra(StaffScreenExtras.EXTRA_IS_VALID, false)
            putExtra(StaffScreenExtras.EXTRA_MESSAGE, message)
            putExtra(StaffScreenExtras.EXTRA_EVENT_ID, selectedEvent?.id.orEmpty())
            putExtra(StaffScreenExtras.EXTRA_EVENT_TITLE, selectedEvent?.label.orEmpty())
            putExtra(StaffScreenExtras.EXTRA_SCAN_PURPOSE_ID, selectedPurpose?.scanPurposeId?.toString().orEmpty())
            putExtra(StaffScreenExtras.EXTRA_SCAN_PURPOSE_NAME, selectedPurpose?.name.orEmpty())
            putExtra(StaffScreenExtras.EXTRA_SCAN_PURPOSE_CODE, selectedPurpose?.code?.name.orEmpty())
            putExtra(StaffScreenExtras.EXTRA_QR_VALUE, qrInput.text.toString().trim())
            putExtra(StaffScreenExtras.EXTRA_STAFF_USER_ID, staffUserId.orEmpty())
        })
    }

    private fun parseQrPayload(raw: String): ParsedQrPayload? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        if (!(trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            return ParsedQrPayload(trimmed, null)
        }

        return runCatching {
            val json = JSONObject(trimmed)
            val qrValue = firstNonBlank(
                json.optString("qrValue"),
                json.optString("qr_value"),
                json.optString("value"),
            )
            val qrCredentialId = firstNonBlank(
                json.optString("qrCredentialId"),
                json.optString("qr_credential_id"),
                json.optString("credentialId"),
            )
            qrValue?.let { ParsedQrPayload(it, qrCredentialId) }
        }.getOrNull()
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }?.trim()

    private data class ParsedQrPayload(
        val qrValue: String,
        val qrCredentialId: String?,
    )
}
