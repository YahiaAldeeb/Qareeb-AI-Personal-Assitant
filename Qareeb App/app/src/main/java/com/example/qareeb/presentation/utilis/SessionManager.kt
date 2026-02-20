package com.example.qareeb.presentation.utilis

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager private constructor(context: Context) {

    private var prefs: SharedPreferences

    init {
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
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val PREFS_NAME = "qareeb_secure_prefs"
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
    }

    fun saveUserId(userId: String) {                          // ← String not Long
        prefs.edit().putString(USER_ID, userId).apply()       // ← putString not putLong
    }

    fun saveUsername(username: String) {
        prefs.edit().putString(USER_NAME, username).apply()
    }

    fun saveUserEmail(email: String) {
        prefs.edit().putString(USER_EMAIL, email).apply()
    }

    fun getUserId(): String? {
        return prefs.getString(USER_ID, null)                 // ← null not -1L
    }

    fun getUsername(): String? {
        return prefs.getString(USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(USER_EMAIL, null)
    }

    fun isLoggedIn(): Boolean {
        return !getUserId().isNullOrEmpty() && !getUsername().isNullOrEmpty() // ← String null check
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}