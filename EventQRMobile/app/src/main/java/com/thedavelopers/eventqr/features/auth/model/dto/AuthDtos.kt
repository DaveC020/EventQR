package com.thedavelopers.eventqr.features.auth.model.dto

import com.thedavelopers.eventqr.core.api.dto.AccountRole
import java.util.UUID

data class LoginRequest(
    val email: String,
    val password: String,
)

data class LoginResponse(
    val accessToken: String,
    val userId: UUID,
    val email: String,
    val fullName: String,
    val role: AccountRole?,
    val message: String? = null,
)
