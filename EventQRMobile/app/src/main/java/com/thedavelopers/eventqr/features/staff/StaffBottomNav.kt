package com.thedavelopers.eventqr.features.staff

import android.content.Intent
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.features.staff.scanner.ScannerActivity

enum class StaffBottomNavItem {
    DASHBOARD,
    SCAN,
    TRANSACTIONS,
    PROFILE,
}

fun AppCompatActivity.configureStaffBottomNav(selectedItem: StaffBottomNavItem) {
    bindBottomNavItem(R.id.navDashboard, selectedItem == StaffBottomNavItem.DASHBOARD, StaffDashboardActivity::class.java)
    bindBottomNavItem(R.id.navScanner, selectedItem == StaffBottomNavItem.SCAN, ScannerActivity::class.java)
    bindBottomNavItem(R.id.navLogs, selectedItem == StaffBottomNavItem.TRANSACTIONS, StaffTransactionsActivity::class.java)
    bindBottomNavItem(R.id.navProfile, selectedItem == StaffBottomNavItem.PROFILE, StaffProfileActivity::class.java)

    styleBottomNavItem(R.id.navDashboard, selectedItem == StaffBottomNavItem.DASHBOARD, "Dashboard", R.drawable.ic_nav_home)
    styleBottomNavItem(R.id.navScanner, selectedItem == StaffBottomNavItem.SCAN, "Scan QR", R.drawable.ic_qr_scan)
    styleBottomNavItem(R.id.navLogs, selectedItem == StaffBottomNavItem.TRANSACTIONS, "Transactions", R.drawable.ic_file)
    styleBottomNavItem(R.id.navProfile, selectedItem == StaffBottomNavItem.PROFILE, "Profile", R.drawable.ic_nav_profile)
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
