package com.thedavelopers.eventqr.features.registrations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.dto.RegistrationStatus
import com.thedavelopers.eventqr.core.util.DateFormatters
import com.thedavelopers.eventqr.features.registrations.model.dto.RegistrationResponse

class RegistrationAdapter(
    private val onClick: ((RegistrationResponse) -> Unit)? = null,
) : RecyclerView.Adapter<RegistrationAdapter.ViewHolder>() {

    private val items = mutableListOf<RegistrationResponse>()

    fun submitItems(newItems: List<RegistrationResponse>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_registration, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener {
            onClick?.invoke(items[position])
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.txtRegistrationTitle)
        private val detailView: TextView = itemView.findViewById(R.id.txtRegistrationDetails)
        private val statusView: TextView = itemView.findViewById(R.id.txtRegistrationStatus)

        fun bind(item: RegistrationResponse) {
            titleView.text = item.attendeeName
            detailView.text = buildString {
                append(item.attendeeEmail)
                append("\nRegistered: ")
                append(DateFormatters.formatInstant(item.registeredAt))
                append("\nQR: ")
                append(item.qrCredentialId?.toString() ?: "Pending")
            }
            statusView.text = item.status.name.replace('_', ' ')
            statusView.setBackgroundResource(
                if (item.status == RegistrationStatus.REGISTERED) R.drawable.button_rounded else R.drawable.transparent_alt
            )
        }
    }
}