// AuthManager.kt — Firebase Auth manager for Android
package com.srcardiocare.core.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

/**
 * Manages authentication state using Firebase Auth.
 * Firebase handles token management automatically (1hr ID tokens, auto-refresh).
 * Local SharedPreferences caches the user role for quick access.
 */
class AuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "sr_cardiocare_prefs",
        Context.MODE_PRIVATE
    )

    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    var userRole: String?
        get() = prefs.getString(KEY_USER_ROLE, null)
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    val isLoggedIn: Boolean get() = auth.currentUser != null

    val currentUID: String? get() = auth.currentUser?.uid

    fun clearAll() {
        prefs.edit().clear().apply()
        auth.signOut()
    }

    /**
     * Validates password meets minimum requirements:
     * 8+ characters, at least one uppercase, one lowercase, one digit.
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        if (!password.any { it.isUpperCase() }) return false
        if (!password.any { it.isLowerCase() }) return false
        if (!password.any { it.isDigit() }) return false
        return true
    }
}
