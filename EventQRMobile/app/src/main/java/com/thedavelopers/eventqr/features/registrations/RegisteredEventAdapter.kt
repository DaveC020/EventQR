package com.thedavelopers.eventqr.features.registrations

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.dto.RegistrationStatus
import com.thedavelopers.eventqr.core.util.DateFormatters
import com.thedavelopers.eventqr.features.attendee.AttendeeTransactionsActivity
import com.thedavelopers.eventqr.features.attendee.EXTRA_EVENT_ID
import com.thedavelopers.eventqr.features.attendee.EXTRA_EVENT_TITLE
import com.thedavelopers.eventqr.features.attendee.EventDetailActivity
import com.thedavelopers.eventqr.features.events.model.dto.AttendeeEventResponse
import com.thedavelopers.eventqr.features.registrations.model.dto.RegistrationResponse

class RegisteredEventAdapter : RecyclerView.Adapter<RegisteredEventAdapter.ViewHolder>() {

    private val items = mutableListOf<Pair<AttendeeEventResponse, RegistrationResponse>>()

    fun submitItems(newItems: List<Pair<AttendeeEventResponse, RegistrationResponse>>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_registered_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position].first, items[position].second)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.txtRegisteredEventTitle)
        private val statusView: TextView = itemView.findViewById(R.id.txtRegisteredEventStatus)
        private val dateTimeView: TextView = itemView.findViewById(R.id.txtEventDateTime)
        private val locationView: TextView = itemView.findViewById(R.id.txtEventLocation)
        private val regDateView: TextView = itemView.findViewById(R.id.txtRegistrationDate)
        private val pointsView: TextView = itemView.findViewById(R.id.txtPoints)
        private val btnTransactions: Button = itemView.findViewById(R.id.btnTransactionHistory)
        private val btnDetails: Button = itemView.findViewById(R.id.btnEventDetails)

        fun bind(event: AttendeeEventResponse, registration: RegistrationResponse) {
            titleView.text = event.title
            statusView.text = registration.status.name.lowercase().replaceFirstChar { it.uppercase() }
            
            val (statusColor, statusBg) = when (registration.status) {
                RegistrationStatus.REGISTERED -> "#4F46E5" to R.drawable.bg_purple_pill
                RegistrationStatus.ENTERED -> "#16A34A" to R.drawable.bg_green_pill
                RegistrationStatus.CANCELLED -> "#EF4444" to R.drawable.bg_red_warning
                else -> "#6B7280" to R.drawable.bg_soft_gray_pill
            }

            statusView.setTextColor(android.graphics.Color.parseColor(statusColor))
            statusView.setBackgroundResource(statusBg)

            dateTimeView.text = DateFormatters.formatInstant(event.eventStartAt)
            locationView.text = event.location ?: "Location not specified"
            regDateView.text = "Registered: ${registration.registeredAt?.let { DateFormatters.formatInstant(it) } ?: "N/A"}"
            
            // Points would ideally come from the registration or a separate balance call
            pointsView.text = "0 pts"

            btnTransactions.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, AttendeeTransactionsActivity::class.java).apply {
                    putExtra(EXTRA_EVENT_ID, event.eventId.toString())
                    putExtra(EXTRA_EVENT_TITLE, event.title)
                }
                context.startActivity(intent)
            }

            btnDetails.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, EventDetailActivity::class.java).apply {
                    putExtra(EXTRA_EVENT_ID, event.eventId.toString())
                    putExtra(EXTRA_EVENT_TITLE, event.title)
                }
                context.startActivity(intent)
            }
        }
    }
}