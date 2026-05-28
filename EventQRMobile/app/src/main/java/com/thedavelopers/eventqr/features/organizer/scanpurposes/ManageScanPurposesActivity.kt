package com.thedavelopers.eventqr.features.organizer.scanpurposes

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.features.organizer.*
import com.thedavelopers.eventqr.features.organizer.model.dto.OrganizerTransactionRuleDto
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

open class ManageScanPurposesActivity : AppCompatActivity() {
    private val TAG = "ManageScanPurposesActivity"
    private lateinit var repository: OrganizerRepository
    private lateinit var selectedEvent: OrganizerMvpEvent
    private lateinit var purposeHost: LinearLayout
    private var refreshCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OrganizerRepository(this)
        val eventId = intentEventId() ?: return showMissingEventScreen("Scan Purposes")
        selectedEvent = resolveSelectedEvent(repository.getApprovedOrganizerEvents(), eventId) ?: return showMissingEventScreen("Scan Purposes")
        
        Log.d(TAG, "Loading scan purposes for eventId: $eventId")
        
        val content = organizerShell(
            title = "Scan Purposes",
            showBack = true,
            topRightLabel = "+ Add",
            onTopRight = { showAddEditDialog() }
        )
        
        purposeHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(purposeHost)
        
        loadPurposes()
    }

    private fun loadPurposes() {
        refreshCount += 1
        purposeHost.removeAllViews()
        purposeHost.addView(loadingState("Loading scan purposes..."))
        MainScope().launch {
            val source = repository.loadScanPurposesForMvp(selectedEvent.id)
            Log.d(
                TAG,
                "eventId=${selectedEvent.id} refreshCount=$refreshCount loadedCount=${source.data.size} source=${source.source} message=${source.message}"
            )
            renderPurposes(source.data)
            source.message?.let {
                Toast.makeText(this@ManageScanPurposesActivity, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderPurposes(purposes: List<OrganizerMvpScanPurpose>) {
        purposeHost.removeAllViews()
        if (purposes.isEmpty()) {
            purposeHost.addView(emptyState("No scan purposes configured yet. Use '+ Add' to create one."))
            return
        }
        
        purposes.forEach { purpose ->
            val subtitle = buildString {
                if (purpose.pointsEnabled) append("+${purpose.pointsValue} pts · ")
                append(if (purpose.duplicateRule.lowercase().contains("allow")) "Allows duplicates" else "No duplicates")
            }
            
            purposeHost.addView(purposeCard(
                title = purpose.label,
                subtitle = subtitle,
                enabled = purpose.enabled,
                onToggle = { isChecked ->
                    togglePurpose(purpose, isChecked)
                }
            ).apply {
                setOnClickListener { showAddEditDialog(purpose) }
            })
        }
    }

    private fun togglePurpose(purpose: OrganizerMvpScanPurpose, enabled: Boolean) {
        MainScope().launch {
            Log.d(
                TAG,
                "eventId=${selectedEvent.id} purposeId=${purpose.id ?: "null"} label=${purpose.label} toggleValue=$enabled"
            )

            if (purpose.id.isNullOrBlank()) {
                val createResult = repository.saveScanPurposesForMvp(
                    selectedEvent.id,
                    listOf(purpose.copy(enabled = enabled))
                )
                Log.d(
                    TAG,
                    "eventId=${selectedEvent.id} purposeId=${purpose.id ?: "null"} toggleCreateResultSource=${createResult.source} message=${createResult.message} returnedCount=${createResult.data.size}"
                )
                if (createResult.source != OrganizerMvpDataSource.BACKEND) {
                    Toast.makeText(
                        this@ManageScanPurposesActivity,
                        "Failed to update: ${createResult.message ?: "Unable to save scan purpose"}",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadPurposes()
                    return@launch
                }
                Toast.makeText(
                    this@ManageScanPurposesActivity,
                    "${purpose.label} ${if (enabled) "enabled" else "disabled"}",
                    Toast.LENGTH_SHORT
                ).show()
                loadPurposes()
                return@launch
            }

            val result = repository.enableScanPurposeForMvp(selectedEvent.id, purpose.id, enabled)
            when (result) {
                is NetworkResult.Success -> {
                    Log.d(
                        TAG,
                        "eventId=${selectedEvent.id} purposeId=${purpose.id} toggleApiResult=SUCCESS active=${result.data.enabled}"
                    )
                    Toast.makeText(
                        this@ManageScanPurposesActivity,
                        "${purpose.label} ${if (enabled) "enabled" else "disabled"}",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadPurposes()
                }
                is NetworkResult.Error -> {
                    Log.w(
                        TAG,
                        "eventId=${selectedEvent.id} purposeId=${purpose.id} toggleApiResult=ERROR message=${result.message}"
                    )
                    Toast.makeText(
                        this@ManageScanPurposesActivity,
                        "Failed to update: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadPurposes()
                }
                NetworkResult.Loading -> {
                    Log.d(
                        TAG,
                        "eventId=${selectedEvent.id} purposeId=${purpose.id} toggleApiResult=LOADING"
                    )
                }
            }
        }
    }

    private fun showAddEditDialog(purpose: OrganizerMvpScanPurpose? = null) {
        val isEdit = purpose != null
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        val nameInput = EditText(this).apply {
            hint = "Purpose Name (e.g. Session Attendance)"
            setText(purpose?.label ?: "")
        }
        val descInput = EditText(this).apply {
            hint = "Description"
            setText(purpose?.description ?: "")
        }
        val pointsInput = EditText(this).apply {
            hint = "Points awarded"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(purpose?.pointsValue?.toString() ?: "0")
        }
        val duplicateCheck = CheckBox(this).apply {
            text = "Allow duplicate scans"
            isChecked = purpose?.duplicateRule?.lowercase()?.contains("allow") ?: false
        }
        val trackingOnlyCheck = CheckBox(this).apply {
            text = "Tracking only (no points)"
            isChecked = purpose?.trackingOnly ?: false
        }

        dialogView.addView(text("Name", 14, true))
        dialogView.addView(nameInput)
        dialogView.addView(text("Description", 14, true).apply { setPadding(0, dp(12), 0, 0) })
        dialogView.addView(descInput)
        dialogView.addView(text("Points", 14, true).apply { setPadding(0, dp(12), 0, 0) })
        dialogView.addView(pointsInput)
        dialogView.addView(duplicateCheck)
        dialogView.addView(trackingOnlyCheck)

        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit Scan Purpose" else "Add Scan Purpose")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newPurpose = (purpose ?: OrganizerMvpScanPurpose(
                    label = "",
                    description = "",
                    enabled = true,
                    duplicateRule = "",
                    trackingOnly = false,
                    pointsEnabled = true,
                    pointsValue = 0,
                    requiredSelectionLabel = ""
                )).copy(
                    label = nameInput.text.toString(),
                    description = descInput.text.toString(),
                    pointsValue = pointsInput.text.toString().toIntOrNull() ?: 0,
                    pointsEnabled = !trackingOnlyCheck.isChecked,
                    trackingOnly = trackingOnlyCheck.isChecked,
                    duplicateRule = if (duplicateCheck.isChecked) "Allow Duplicates" else "No Duplicates"
                )
                savePurpose(newPurpose)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun savePurpose(purpose: OrganizerMvpScanPurpose) {
        MainScope().launch {
            val source = repository.saveScanPurposesForMvp(selectedEvent.id, listOf(purpose))
            Log.d(TAG, "Save purpose ${purpose.label} result: ${source.source}")
            if (source.source == OrganizerMvpDataSource.BACKEND) {
                Toast.makeText(this@ManageScanPurposesActivity, "Saved successfully", Toast.LENGTH_SHORT).show()
                loadPurposes()
            } else {
                Toast.makeText(this@ManageScanPurposesActivity, "Failed to save: ${source.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
