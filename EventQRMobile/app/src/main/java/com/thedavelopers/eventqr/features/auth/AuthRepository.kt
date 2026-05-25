package com.thedavelopers.eventqr.features.auth

import android.content.Context
import com.thedavelopers.eventqr.core.api.ApiClient
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.api.safeApiCall
import com.thedavelopers.eventqr.core.api.dto.AccountRole
import com.thedavelopers.eventqr.core.session.SessionManager
import com.thedavelopers.eventqr.features.auth.model.dto.LoginRequest
import com.thedavelopers.eventqr.features.auth.model.dto.LoginResponse
import com.thedavelopers.eventqr.features.users.model.dto.UserRequest
import com.thedavelopers.eventqr.features.users.model.dto.UserResponse

class AuthRepository(context: Context) {
    private val apiService = ApiClient.getService(context)
    private val sessionManager = SessionManager(context)

    suspend fun login(email: String, password: String): NetworkResult<LoginResponse> =
        safeApiCall { apiService.login(LoginRequest(email, password)) }

    suspend fun createUser(
        fullName: String,
        email: String,
        phoneNumber: String,
        password: String,
        role: AccountRole = AccountRole.ATTENDEE,
    ): NetworkResult<UserResponse> =
        safeApiCall {
            apiService.createUser(
                UserRequest(
                    email = email,
                    fullName = fullName,
                    phoneNumber = phoneNumber.ifBlank { null },
                    password = password,
                    role = role,
                )
            )
        }

    fun storeSession(loginResponse: LoginResponse) {
        sessionManager.saveLoginResponse(loginResponse)
    }

    fun saveUserRole(role: AccountRole?) {
        sessionManager.saveRole(role)
    }
}
