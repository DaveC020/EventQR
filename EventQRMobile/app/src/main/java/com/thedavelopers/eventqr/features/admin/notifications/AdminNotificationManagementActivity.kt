package com.thedavelopers.eventqr.features.admin.notifications

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.features.admin.AdminEventApprovalBackendActivity
import com.thedavelopers.eventqr.features.admin.dashboard.AdminDashboardActivity
import com.thedavelopers.eventqr.features.admin.logs.AdminAuditLogsActivity
import com.thedavelopers.eventqr.features.admin.users.AdminAccountManagementActivity

class AdminNotificationManagementActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_notification_management)
        bindNavigation()
    }

    private fun bindNavigation() {
        findViewById<View>(R.id.navDashboard).setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navRequests).setOnClickListener {
            startActivity(Intent(this, AdminEventApprovalBackendActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navAccounts).setOnClickListener {
            startActivity(Intent(this, AdminAccountManagementActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navLogs).setOnClickListener {
            startActivity(Intent(this, AdminAuditLogsActivity::class.java))
            finish()
        }
    }
}
