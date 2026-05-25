package com.thedavelopers.eventqr.core.util

object Validators {
    fun isValidEmail(value: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()
    }

    fun isValidPassword(value: String): Boolean {
        return value.trim().length >= 8
    }

    fun isValidPhoneNumber(value: String): Boolean {
        return value.trim().length >= 7
    }

    fun isNonEmpty(value: String): Boolean {
        return value.trim().isNotEmpty()
    }
}
