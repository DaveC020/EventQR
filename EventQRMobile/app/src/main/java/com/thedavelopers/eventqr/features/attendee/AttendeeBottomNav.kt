package com.thedavelopers.eventqr.features.attendee

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.features.dashboard.DashboardActivity

enum class AttendeeBottomNavItem {
    DASHBOARD,
    EVENTS,
    MY_EVENTS,
    REWARDS,
    PROFILE,
}

fun AppCompatActivity.configureAttendeeBottomNav(selectedItem: AttendeeBottomNavItem) {
    bindBottomNavItem(R.id.navDashboard, selectedItem == AttendeeBottomNavItem.DASHBOARD, DashboardActivity::class.java)
    bindBottomNavItem(R.id.navEvents, selectedItem == AttendeeBottomNavItem.EVENTS, AttendeeEventsActivity::class.java)
    bindBottomNavItem(R.id.navMyEvents, selectedItem == AttendeeBottomNavItem.MY_EVENTS, RegisteredEventsActivity::class.java)
    bindBottomNavItem(R.id.navRewards, selectedItem == AttendeeBottomNavItem.REWARDS, AttendeeRewardsActivity::class.java)
    bindBottomNavItem(R.id.navProfile, selectedItem == AttendeeBottomNavItem.PROFILE, AttendeeProfileActivity::class.java)

    styleBottomNavItem(R.id.navDashboard, selectedItem == AttendeeBottomNavItem.DASHBOARD, "Dashboard", R.drawable.ic_nav_home)
    styleBottomNavItem(R.id.navEvents, selectedItem == AttendeeBottomNavItem.EVENTS, "Events", R.drawable.ic_nav_calendar)
    styleBottomNavItem(R.id.navMyEvents, selectedItem == AttendeeBottomNavItem.MY_EVENTS, "My Events", R.drawable.ic_nav_calendar)
    styleBottomNavItem(R.id.navRewards, selectedItem == AttendeeBottomNavItem.REWARDS, "Rewards", R.drawable.ic_nav_gift)
    styleBottomNavItem(R.id.navProfile, selectedItem == AttendeeBottomNavItem.PROFILE, "Profile", R.drawable.ic_nav_profile)
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
        if (selected) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#000000")
    )
    title.text = label
    title.setTextColor(if (selected) android.graphics.Color.parseColor("#312E81") else android.graphics.Color.parseColor("#6B7280"))
    title.setTypeface(null, if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
}
