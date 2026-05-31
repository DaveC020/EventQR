package com.thedavelopers.eventqr.features.attendee

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.dto.NotificationStatus
import com.thedavelopers.eventqr.features.notifications.NotificationAdapter
import com.thedavelopers.eventqr.features.notifications.model.dto.NotificationResponse

open class AttendeeNotificationsActivity : AppCompatActivity(), NotificationsContract.View {
    private lateinit var presenter: NotificationsPresenter
    private lateinit var adapter: NotificationAdapter
    private lateinit var recyclerNotifications: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressLoading: ProgressBar
    private lateinit var txtEmpty: TextView
    private lateinit var txtError: TextView
    private lateinit var btnRetry: Button
    private lateinit var actionMarkAllRead: TextView
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        presenter = NotificationsPresenter(this, AttendeeRepository(this))
        adapter = NotificationAdapter { notification ->
            if (notification.status != NotificationStatus.READ) {
                presenter.markRead(notification.notificationId.toString())
            }
        }
        swipeRefresh = findViewById(R.id.swipeRefreshNotifications)
        recyclerNotifications = findViewById(R.id.recyclerNotifications)
        progressLoading = findViewById(R.id.progressNotificationsLoading)
        txtEmpty = findViewById(R.id.txtNotificationsEmpty)
        txtError = findViewById(R.id.txtNotificationsError)
        btnRetry = findViewById(R.id.btnNotificationsRetry)
        actionMarkAllRead = findViewById(R.id.txtMarkAllRead)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }
        actionMarkAllRead.setOnClickListener { presenter.markAllRead() }
        btnRetry.setOnClickListener { presenter.load() }
        swipeRefresh.setOnRefreshListener { presenter.load() }

        recyclerNotifications.apply {
            layoutManager = LinearLayoutManager(this@AttendeeNotificationsActivity)
            adapter = this@AttendeeNotificationsActivity.adapter
        }

        presenter.load()
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    override fun showLoading(isLoading: Boolean) {
        if (!swipeRefresh.isRefreshing) {
            progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        if (isLoading) {
            recyclerNotifications.visibility = View.GONE
            txtEmpty.visibility = View.GONE
            txtError.visibility = View.GONE
            btnRetry.visibility = View.GONE
        } else {
            swipeRefresh.isRefreshing = false
        }
    }

    override fun showContent() {
        swipeRefresh.isRefreshing = false
        txtError.visibility = View.GONE
        btnRetry.visibility = View.GONE
        progressLoading.visibility = View.GONE
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showError(message: String) {
        swipeRefresh.isRefreshing = false
        recyclerNotifications.visibility = View.GONE
        txtEmpty.visibility = View.GONE
        progressLoading.visibility = View.GONE

        txtError.visibility = View.VISIBLE
        btnRetry.visibility = View.VISIBLE
        txtError.text = message
    }

    override fun setMarkAllEnabled(enabled: Boolean) {
        actionMarkAllRead.isEnabled = enabled
        actionMarkAllRead.alpha = if (enabled) 1f else 0.5f
    }

    override fun renderNotifications(items: List<NotificationResponse>) {
        swipeRefresh.isRefreshing = false
        txtError.visibility = View.GONE
        btnRetry.visibility = View.GONE
        progressLoading.visibility = View.GONE

        if (items.isEmpty()) {
            recyclerNotifications.visibility = View.GONE
            txtEmpty.visibility = View.VISIBLE
            txtEmpty.text = "No notifications yet."
            return
        }

        txtEmpty.visibility = View.GONE
        recyclerNotifications.visibility = View.VISIBLE
        adapter.submitItems(items)
    }
}
