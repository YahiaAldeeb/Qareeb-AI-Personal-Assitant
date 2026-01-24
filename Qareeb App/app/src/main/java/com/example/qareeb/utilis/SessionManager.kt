package com.example.qareeb.utilis

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * SessionManager - Manages user session data securely
 * Singleton class to handle user authentication state across the app
 */
class SessionManager private constructor(context: Context) {

    private var prefs: SharedPreferences

    init {
        // Initialize encrypted SharedPreferences for security
        val appContext = context.applicationContext
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val PREFS_NAME = "qareeb_secure_prefs"
        private const val USER_ID = "user_id"
        private const val USER_NAME = "user_name"
        private const val USER_EMAIL = "user_email" // Optional: if you need email

        @Volatile
        private var instance: SessionManager? = null

        /**
         * Get singleton instance of SessionManager
         * Thread-safe double-checked locking
         */
        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * Save complete user session data
     * @param userId User's unique identifier
     * @param username User's display name
     */
    fun saveUserSession(userId: Long, username: String, email: String? = null) {
        prefs.edit().apply {
            putLong(USER_ID, userId)
            putString(USER_NAME, username)
            email?.let { putString(USER_EMAIL, it) }
            apply()
        }
    }

    /**
     * Save user ID
     */
    fun saveUserId(userId: Long) {
        prefs.edit().putLong(USER_ID, userId).apply()
    }

    /**
     * Save username
     */
    fun saveUsername(username: String) {
        prefs.edit().putString(USER_NAME, username).apply()
    }

    /**
     * Save user email
     */
    fun saveUserEmail(email: String) {
        prefs.edit().putString(USER_EMAIL, email).apply()
    }

    /**
     * Get user ID
     * @return User ID or -1 if not found
     */
    fun getUserId(): Long {
        return prefs.getLong(USER_ID, -1L)
    }

    /**
     * Get username
     * @return Username or null if not found
     */
    fun getUsername(): String? {
        return prefs.getString(USER_NAME, null)
    }

    /**
     * Get user email
     * @return Email or null if not found
     */
    fun getUserEmail(): String? {
        return prefs.getString(USER_EMAIL, null)
    }

    /**
     * Check if user is logged in
     * @return true if user session exists, false otherwise
     */
    fun isLoggedIn(): Boolean {
        return getUserId() != -1L && !getUsername().isNullOrEmpty()
    }

    /**
     * Clear all session data (logout)
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }



}