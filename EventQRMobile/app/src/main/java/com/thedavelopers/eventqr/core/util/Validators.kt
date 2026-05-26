package com.thedavelopers.eventqr.core.util

object Validators {
    data class PasswordRequirements(
        val hasMinLength: Boolean,
        val hasCapital: Boolean,
        val hasSpecial: Boolean,
        val hasNumber: Boolean,
    ) {
        val isValid: Boolean
            get() = hasMinLength && hasCapital && hasSpecial && hasNumber
    }

    fun isValidEmail(value: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()
    }

    fun isValidPassword(value: String): Boolean {
        return value.trim().length >= 8
    }

    fun isValidPhoneNumber(value: String): Boolean {
        val cleaned = value.trim()
        return cleaned.startsWith("63") && cleaned.length == 12 && cleaned.all { it.isDigit() }
    }

    fun isNonEmpty(value: String): Boolean {
        return value.trim().isNotEmpty()
    }

    fun passwordRequirements(value: String): PasswordRequirements {
        return PasswordRequirements(
            hasMinLength = value.length >= 8,
            hasCapital = value.any { it.isUpperCase() },
            hasSpecial = value.any { !it.isLetterOrDigit() },
            hasNumber = value.any { it.isDigit() },
        )
    }

    fun isValidSignUpPassword(value: String): Boolean {
        return passwordRequirements(value).isValid
    }
}
