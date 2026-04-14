package com.grocart.first.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("GroCartPrefs", Context.MODE_PRIVATE)


    /**
     * Saves user session details to SharedPreferences.
     * @param userId ID of the logged-in user (Firebase UID).
     * @param username Username.
     */
    fun saveUserSession(userId: String, username: String) {
        prefs.edit {
            putString("USER_ID", userId)
            putString("USERNAME", username)
        }
    }


    /**
     * Gets the current user ID.
     * @return User ID or null if not logged in.
     */
    fun getUserId(): String? = prefs.getString("USER_ID", null)

    /**
     * Gets the current user name.
     * @return Username or null if not logged in.
     */
    fun getUsername(): String? = prefs.getString("USERNAME", null)


    /**
     * Saves the user's delivery address.
     */
    fun saveAddress(address: String) {
        prefs.edit { putString("USER_ADDRESS", address) }
    }

    /**
     * Gets the saved delivery address.
     */
    fun getAddress(): String = prefs.getString("USER_ADDRESS", "Sector-4, GroCart City") ?: "Sector-4, GroCart City"

    /**
     * Clears all session data, effectively logging out the user.
     */
    fun logout() {
        prefs.edit().clear().apply()
    }
}