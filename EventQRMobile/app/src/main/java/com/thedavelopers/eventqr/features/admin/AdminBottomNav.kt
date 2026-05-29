package com.thedavelopers.eventqr.features.admin

import android.content.Intent
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.features.admin.dashboard.AdminDashboardActivity
import com.thedavelopers.eventqr.features.admin.logs.AdminAuditLogsActivity
import com.thedavelopers.eventqr.features.admin.users.AdminAccountManagementActivity

enum class AdminBottomNavItem {
    DASHBOARD,
    REQUESTS,
    ACCOUNTS,
    LOGS,
}

fun AppCompatActivity.configureAdminBottomNav(selectedItem: AdminBottomNavItem) {
    bindBottomNavItem(R.id.navDashboard, selectedItem == AdminBottomNavItem.DASHBOARD, AdminDashboardActivity::class.java)
    bindBottomNavItem(R.id.navRequests, selectedItem == AdminBottomNavItem.REQUESTS, AdminEventApprovalBackendActivity::class.java)
    bindBottomNavItem(R.id.navAccounts, selectedItem == AdminBottomNavItem.ACCOUNTS, AdminAccountManagementActivity::class.java)
    bindBottomNavItem(R.id.navLogs, selectedItem == AdminBottomNavItem.LOGS, AdminAuditLogsActivity::class.java)

    styleBottomNavItem(R.id.navDashboard, selectedItem == AdminBottomNavItem.DASHBOARD, "Dashboard", R.drawable.ic_nav_home)
    styleBottomNavItem(R.id.navRequests, selectedItem == AdminBottomNavItem.REQUESTS, "Requests", R.drawable.ic_group)
    styleBottomNavItem(R.id.navAccounts, selectedItem == AdminBottomNavItem.ACCOUNTS, "Accounts", R.drawable.ic_nav_profile)
    styleBottomNavItem(R.id.navLogs, selectedItem == AdminBottomNavItem.LOGS, "Logs", R.drawable.ic_file)
}

private fun AppCompatActivity.bindBottomNavItem(
    navId: Int,
    isCurrent: Boolean,
    destination: Class<out AppCompatActivity>,
) {
    findViewById<View>(navId)?.apply {
        isClickable = true
        isFocusable = true
        setOnClickListener {
            if (isCurrent) return@setOnClickListener
            startActivity(
                Intent(this@bindBottomNavItem, destination)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            finish()
        }
    }
}

private fun AppCompatActivity.styleBottomNavItem(navId: Int, selected: Boolean, label: String, iconRes: Int) {
    val container = findViewById<View>(navId) as? ViewGroup ?: return
    val icon = container.getChildAt(0) as? ImageView ?: return
    val title = container.getChildAt(1) as? TextView ?: return

    icon.setImageResource(iconRes)
    icon.background = getDrawable(if (selected) R.drawable.bg_nav_icon_active else R.drawable.bg_nav_icon_inactive)
    icon.imageTintList = android.content.res.ColorStateList.valueOf(
        if (selected) android.graphics.Color.WHITE else android.graphics.Color.BLACK
    )
    title.text = label
    title.setTextColor(if (selected) android.graphics.Color.parseColor("#312E81") else android.graphics.Color.parseColor("#6B7280"))
    title.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
}
