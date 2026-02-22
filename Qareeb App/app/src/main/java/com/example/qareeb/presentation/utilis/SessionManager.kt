package com.example.qareeb.presentation.utilis

import android.content.Context
import android.content.SharedPreferences

class SessionManager private constructor(context: Context) {

    private val prefs: SharedPreferences

    init {
        val appContext = context.applicationContext

        // One-time migration: delete old corrupted encrypted prefs if they exist
        try {
            val encryptedFile = java.io.File(
                appContext.filesDir.parent,
                "shared_prefs/qareeb_secure_prefs.xml"
            )
            if (encryptedFile.exists()) {
                encryptedFile.delete()
                android.util.Log.d("SESSION", "Deleted old encrypted prefs file")
            }
        } catch (e: Exception) {
            android.util.Log.e("SESSION", "Could not delete old prefs: ${e.message}")
        }

        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        android.util.Log.d("SESSION", "SessionManager initialized with prefs: $PREFS_NAME")
    }

    companion object {
        private const val PREFS_NAME = "qareeb_session_prefs" // ← new name, fresh start
        private const val USER_ID = "user_id"
        private const val USER_NAME = "user_name"
        private const val USER_EMAIL = "user_email"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    fun saveUserSession(userId: String, username: String, email: String? = null) {
        prefs.edit().apply {
            putString(USER_ID, userId)
            putString(USER_NAME, username)
            email?.let { putString(USER_EMAIL, it) }
            apply()
        }
        android.util.Log.d("SESSION", "Saved session → userId=$userId, username=$username")
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString(USER_ID, userId).apply()
        android.util.Log.d("SESSION", "Saved userId: $userId")
    }

    fun saveUsername(username: String) {
        prefs.edit().putString(USER_NAME, username).apply()
    }

    fun saveUserEmail(email: String) {
        prefs.edit().putString(USER_EMAIL, email).apply()
    }

    fun getUserId(): String? {
        val id = prefs.getString(USER_ID, null)
        android.util.Log.d("SESSION", "getUserId → $id")
        return id
    }

    fun getUsername(): String? {
        return prefs.getString(USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(USER_EMAIL, null)
    }

    fun isLoggedIn(): Boolean {
        return !getUserId().isNullOrEmpty() && !getUsername().isNullOrEmpty()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        android.util.Log.d("SESSION", "Session cleared")
    }
}