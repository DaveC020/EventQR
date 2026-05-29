package com.thedavelopers.eventqr.features.admin.users

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.features.admin.AdminEventApprovalBackendActivity
import com.thedavelopers.eventqr.features.admin.AdminBottomNavItem
import com.thedavelopers.eventqr.features.admin.AdminRepository
import com.thedavelopers.eventqr.features.admin.dashboard.AdminDashboardActivity
import com.thedavelopers.eventqr.features.admin.logs.AdminAuditLogsActivity
import com.thedavelopers.eventqr.features.admin.configureAdminBottomNav
import com.thedavelopers.eventqr.features.users.model.dto.UserResponse
import kotlinx.coroutines.launch

class AdminAccountManagementActivity : AppCompatActivity() {
    private lateinit var repository: AdminRepository
    private lateinit var adapter: AdminAccountAdapter
    private lateinit var searchInput: EditText
    private lateinit var recyclerAccounts: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var textPlaceholder: TextView

    private var allUsers: List<UserResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_account_management)

        repository = AdminRepository(this)
        adapter = AdminAccountAdapter()
        bindViews()
        configureAdminBottomNav(AdminBottomNavItem.ACCOUNTS)
        bindSearch()
        loadAccounts()
    }

    private fun bindViews() {
        searchInput = findViewById(R.id.inputAccountSearch)
        recyclerAccounts = findViewById(R.id.recyclerAdminAccounts)
        progressLoading = findViewById(R.id.progressAccountsLoading)
        textPlaceholder = findViewById(R.id.textAccountsPlaceholder)
        recyclerAccounts.layoutManager = LinearLayoutManager(this)
        recyclerAccounts.adapter = adapter
    }

    private fun bindSearch() {
        searchInput.addTextChangedListener { editable ->
            val query = editable?.toString().orEmpty().trim()
            val filtered = if (query.isBlank()) {
                allUsers
            } else {
                allUsers.filter { user ->
                    user.fullName.contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true) ||
                        user.role.name.contains(query, ignoreCase = true)
                }
            }
            adapter.submitItems(filtered)
            textPlaceholder.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            textPlaceholder.text = if (allUsers.isEmpty()) {
                "Account management is not configured yet."
            } else {
                "No accounts match your search."
            }
        }
    }

    private fun loadAccounts() {
        progressLoading.visibility = View.VISIBLE
        recyclerAccounts.visibility = View.GONE
        textPlaceholder.visibility = View.GONE

        lifecycleScope.launch {
            when (val result = repository.loadUsers()) {
                is NetworkResult.Success -> {
                    allUsers = result.data.sortedBy { it.fullName.lowercase() }
                    progressLoading.visibility = View.GONE
                    recyclerAccounts.visibility = if (allUsers.isEmpty()) View.GONE else View.VISIBLE
                    textPlaceholder.visibility = if (allUsers.isEmpty()) View.VISIBLE else View.GONE
                    textPlaceholder.text = "Account management is not configured yet."
                    adapter.submitItems(allUsers)
                }
                is NetworkResult.Error -> {
                    allUsers = emptyList()
                    progressLoading.visibility = View.GONE
                    recyclerAccounts.visibility = View.GONE
                    textPlaceholder.visibility = View.VISIBLE
                    textPlaceholder.text = "Account management is not configured yet."
                }
                NetworkResult.Loading -> Unit
            }
        }
    }
}
